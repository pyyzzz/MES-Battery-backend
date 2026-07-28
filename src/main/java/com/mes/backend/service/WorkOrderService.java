// 작업지시(WorkOrder) 화면 조회/등록 - assignWorkToMachine()/reportProduction()의 실물 생산 흐름과는 별개
package com.mes.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.WorkOrderRegisterRequest;
import com.mes.backend.entity.Bom;
import com.mes.backend.entity.BomItem;
import com.mes.backend.entity.Employee;
import com.mes.backend.entity.WorkOrder;
import com.mes.backend.exception.CustomException;
import com.mes.backend.repository.BomItemRepository;
import com.mes.backend.repository.BomRepository;
import com.mes.backend.repository.EmployeeRepository;
import com.mes.backend.repository.MaterialLotRepository;
import com.mes.backend.repository.WorkOrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository orderRepo;
    private final BomRepository bomRepo;
    private final BomItemRepository bomItemRepo;
    private final EmployeeRepository employeeRepo;
    private final MaterialLotRepository materialLotRepo;

    public List<WorkOrder> search(String workOrderNo, String workOrderStatus, LocalDate dueDate) {
        return orderRepo.search(workOrderNo, workOrderStatus, dueDate);
    }

    public WorkOrder getById(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("작업지시를 찾을 수 없습니다. ID: " + id));
    }

    /* manager_employee_id는 폼 입력이 아니라 로그인 세션(username)으로 조회한 Employee를 그대로 사용 */
    @PreAuthorize("hasAuthority('관리자')")
    @Transactional
    public WorkOrder create(WorkOrderRegisterRequest request, String username) {
        Bom bom = bomRepo.findById(request.getBomId())
                .orElseThrow(() -> new RuntimeException("BOM을 찾을 수 없습니다. ID: " + request.getBomId()));
        Employee manager = employeeRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("계정을 찾을 수 없습니다: " + username));

        validateMaterialStock(bom, request.getOrderQuantity());

        return orderRepo.save(WorkOrder.builder()
                .bom(bom)
                .managerEmployee(manager)
                .workOrderNo(generateWorkOrderNo())
                .orderQuantity(request.getOrderQuantity())
                .dueDate(request.getDueDate())
                .workOrderStatus("WAITING")
                .build());
    }

    /* "WO-" + 현재분(yyMMddHHmm) + "-" + 순번, 순번은 같은 분 안 기존 건수+1 (ProductLot 채번과 동일 방식) */
    private String generateWorkOrderNo() {
        String prefix = "WO-" + DateTimeFormatter.ofPattern("yyMMddHHmm").format(LocalDateTime.now());
        long sequence = orderRepo.countByWorkOrderNoStartingWith(prefix) + 1;
        return prefix + "-" + sequence;
    }

    private void validateMaterialStock(Bom bom, Integer orderQuantity) {
        int quantity = orderQuantity != null ? orderQuantity : 0;
        if (quantity <= 0) {
            throw new IllegalArgumentException("작업지시 수량은 1 이상이어야 합니다.");
        }

        Map<Long, RequiredMaterial> requiredByMaterialId = new LinkedHashMap<>();
        for (BomItem item : bomItemRepo.findAllByBom(bom)) {
            if (item.getMaterial() == null
                    || item.getMaterial().getId() == null
                    || item.getRequiredQuantity() == null) {
                continue;
            }

            BigDecimal requiredQuantity = item.getRequiredQuantity().multiply(BigDecimal.valueOf(quantity));
            requiredByMaterialId.merge(
                    item.getMaterial().getId(),
                    new RequiredMaterial(
                            item.getMaterial().getId(),
                            item.getMaterial().getMaterialCode(),
                            item.getMaterial().getMaterialName(),
                            requiredQuantity
                    ),
                    (left, right) -> left.add(right.requiredQuantity())
            );
        }

        if (requiredByMaterialId.isEmpty()) {
            throw new CustomException("INVALID_BOM", "선택한 제품에 등록된 BOM 자재가 없어 등록할 수 없습니다.");
        }

        for (RequiredMaterial requiredMaterial : requiredByMaterialId.values()) {
            BigDecimal availableQuantity = materialLotRepo.sumCurrentQuantityByMaterialId(requiredMaterial.materialId());
            if (availableQuantity.compareTo(requiredMaterial.requiredQuantity()) < 0) {
                throw new CustomException(
                        "SHORTAGE",
                        "자재 재고가 부족합니다. "
                                + requiredMaterial.materialName()
                                + " 필요 수량: " + requiredMaterial.requiredQuantity()
                                + ", 현재 재고: " + availableQuantity
                );
            }
        }
    }

    private record RequiredMaterial(Long materialId, String materialCode, String materialName, BigDecimal requiredQuantity) {
        private RequiredMaterial add(BigDecimal additionalQuantity) {
            return new RequiredMaterial(materialId, materialCode, materialName, requiredQuantity.add(additionalQuantity));
        }
    }
}
