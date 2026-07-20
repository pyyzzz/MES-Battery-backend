package com.mes.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.RecentLogDto;
import com.mes.backend.entity.Bom;
import com.mes.backend.entity.Material;
import com.mes.backend.entity.ProductionLog;
import com.mes.backend.entity.WorkOrder;
import com.mes.backend.exception.CustomException;
import com.mes.backend.repository.BomRepository;
import com.mes.backend.repository.MaterialRepository;
import com.mes.backend.repository.ProductionLogRepository;
import com.mes.backend.repository.WorkOrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionService {

    private final WorkOrderRepository orderRepo;
    private final MaterialRepository materialRepo;
    private final BomRepository bomRepo;
    private final ProductionLogRepository logRepo;

    @Transactional
    public Material inboundMaterial(String code, String name, int amount) {
        Material material = materialRepo.findByCode(code)
                .orElse(Material.builder().code(code).name(name).currentStock(0).build());
        material.setCurrentStock(material.getCurrentStock() + amount);
        return materialRepo.save(material);
    }

    public List<Material> getMaterialStock() {
        return materialRepo.findAll();
    }

    @Transactional
    public WorkOrder createWorkOrder(String productCode, int targetQty) {
        WorkOrder order = WorkOrder.builder()
                .productCode(productCode)
                .targetQty(targetQty)
                .currentQty(0)
                .status("WAITING")
                .build();
        return orderRepo.save(order);
    }

    public List<WorkOrder> getAllWorkOrders() {
        return orderRepo.findAllByOrderByIdDesc();
    }

    @Transactional
    public WorkOrder assignWorkToMachine(String machineId) {
        return orderRepo.findByStatusAndAssignedMachineId("IN_PROGRESS", machineId)
                .orElseGet(() -> {
                    WorkOrder waiting = orderRepo.findFirstByStatusOrderByIdAsc("WAITING").orElse(null);
                    if (waiting != null) {
                        if (!isMaterialAvailable(waiting.getProductCode())) {
                            log.warn("[할당 보류] {} - 자재 부족으로 할당하지 않음", waiting.getProductCode());
                            return null;
                        }
                        waiting.setStatus("IN_PROGRESS");
                        waiting.setAssignedMachineId(machineId);
                        return orderRepo.save(waiting);
                    }
                    return null;
                });
    }

    private boolean isMaterialAvailable(String productCode) {
        List<Bom> boms = bomRepo.findAllByProductCode(productCode);
        for (Bom bom : boms) {
            if (bom.getMaterial().getCurrentStock() < bom.getRequiredQty()) {
                log.error("자재 부족: {} (현재: {}, 필요: {})",
                        bom.getMaterial().getName(), bom.getMaterial().getCurrentStock(), bom.getRequiredQty());
                return false;
            }
        }
        return true;
    }

    @Transactional
    public void reportProduction(Long orderId, String machineId, String result, String defectCode, String serialNo) {
        WorkOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("작업 지시를 찾을 수 없습니다. ID: " + orderId));
        if ("COMPLETED".equals(order.getStatus())) return;

        logRepo.save(ProductionLog.builder()
                .workOrder(order).productCode(order.getProductCode())
                .machineId(machineId).serialNo(serialNo)
                .result(result).defectCode("NG".equals(result) ? defectCode : null)
                .productAt(LocalDateTime.now()).build());

        // Backflushing: 생산완료 이벤트 발생 후 BOM 역산해서 자재 재고 사후 차감 (원본 그대로, 판정 로직 없음)
        if ("OK".equals(result)) {
            List<Bom> boms = bomRepo.findAllByProductCode(order.getProductCode());
            for (Bom bom : boms) {
                Material mat = bom.getMaterial();
                if (mat.getCurrentStock() < bom.getRequiredQty())
                    throw new CustomException("SHORTAGE", "MATERIAL_SHORTAGE:" + mat.getName());
                mat.setCurrentStock(mat.getCurrentStock() - bom.getRequiredQty());
            }
        }
        order.setCurrentQty(order.getCurrentQty() + 1);
        if (order.getCurrentQty() >= order.getTargetQty()) order.setStatus("COMPLETED");
    }

    public List<RecentLogDto> getRecentLogs() {
        return logRepo.findTop15ByOrderByIdDesc().stream()
                .map(l -> new RecentLogDto(l.getId(), l.getSerialNo(), l.getResult(), l.getMachineId(), l.getProductAt()))
                .collect(Collectors.toList());
    }
}
