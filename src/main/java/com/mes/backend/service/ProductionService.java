package com.mes.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.ProductionReportDto;
import com.mes.backend.entity.Bom;
import com.mes.backend.entity.BomItem;
import com.mes.backend.entity.DefectType;
import com.mes.backend.entity.Equipment;
import com.mes.backend.entity.Material;
import com.mes.backend.entity.MaterialLot;
import com.mes.backend.entity.MeasurementSpec;
import com.mes.backend.entity.Process;
import com.mes.backend.entity.ProductLot;
import com.mes.backend.entity.QualityInspection;
import com.mes.backend.entity.WorkOrder;
import com.mes.backend.exception.CustomException;
import com.mes.backend.repository.BomItemRepository;
import com.mes.backend.repository.BomRepository;
import com.mes.backend.repository.InspectionMeasurementRepository;
import com.mes.backend.repository.MaterialLotRepository;
import com.mes.backend.repository.MaterialRepository;
import com.mes.backend.repository.MaterialTransactionRepository;
import com.mes.backend.repository.MeasurementSpecRepository;
import com.mes.backend.repository.ProcessRepository;
import com.mes.backend.repository.ProductLotRepository;
import com.mes.backend.repository.QualityInspectionRepository;
import com.mes.backend.repository.WorkOrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionService {

    private static final int PROCESS_TYPE_INSPECTION = 53; // 0x35
    private static final int PROCESS_TYPE_PACKAGING = 54;  // 0x36

    private final WorkOrderRepository orderRepo;
    private final MaterialRepository materialRepo;
    private final BomRepository bomRepo;

    private final ProcessRepository processRepo;
    private final BomItemRepository bomItemRepo;
    private final ProductLotRepository productLotRepo;
    private final MaterialLotRepository materialLotRepo;
    private final MaterialTransactionRepository materialTransactionRepo;
    private final QualityInspectionRepository qualityInspectionRepo;
    private final InspectionMeasurementRepository inspectionMeasurementRepo;
    private final MeasurementSpecRepository measurementSpecRepo;

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

    /* 라인이 1개뿐이라 시스템 전체에 동시 진행 중인 작업지시는 최대 1개로 가정(machineId별 할당 개념 없음) */
    @Transactional
    public WorkOrder assignWorkToMachine(String machineId) {
        WorkOrder order = orderRepo.findFirstByWorkOrderStatus("IN_PROGRESS")
                .orElseGet(() -> orderRepo.findFirstByWorkOrderStatusOrderByIdAsc("WAITING")
                        .map(waiting -> {
                            String productCode = waiting.getBom() != null && waiting.getBom().getProduct() != null
                                    ? waiting.getBom().getProduct().getProductCode() : null;
                            if (!isMaterialAvailable(productCode)) {
                                log.warn("[할당 보류] {} - 자재 부족으로 할당하지 않음", productCode);
                                return null;
                            }
                            waiting.setWorkOrderStatus("IN_PROGRESS");
                            waiting.setActualStartAt(LocalDateTime.now());
                            WorkOrder saved = orderRepo.save(waiting);
                            ensureProductLot(saved);
                            return saved;
                        })
                        .orElse(null));

        if (order == null) {
            return null;
        }
        populateTransientFields(order);
        return order;
    }

    private void ensureProductLot(WorkOrder order) {
        if (order.getProductLot() != null) {
            return;
        }
        String prefix = "LOT-" + DateTimeFormatter.ofPattern("yyMMddHHmm").format(LocalDateTime.now());
        long sequence = productLotRepo.countByProductLotNoStartingWith(prefix) + 1;
        productLotRepo.save(ProductLot.builder()
                .workOrder(order)
                .productLotNo(prefix + "-" + sequence)
                .currentQty(0)
                .lotStatus("IN_PROGRESS")
                .build());
    }

    private void populateTransientFields(WorkOrder order) {
        if (order.getBom() != null && order.getBom().getProduct() != null) {
            order.setProductCode(order.getBom().getProduct().getProductCode());
        }
        order.setTargetQty(order.getOrderQuantity() != null ? order.getOrderQuantity() : 0);
        order.setCurrentQty(order.getProductLot() != null && order.getProductLot().getCurrentQty() != null
                ? order.getProductLot().getCurrentQty() : 0);
        order.setStatus(order.getWorkOrderStatus());
    }

    private boolean isMaterialAvailable(String productCode) {
        if (productCode == null || productCode.trim().isEmpty()) {
            log.warn("[자재 확인 실패] productCode가 비어있습니다.");
            return false;
        }

        List<Bom> boms = bomRepo.findAllByProduct_ProductCode(productCode);
        if (boms.isEmpty()) {
            log.warn("[자재 확인 실패] 제품에 연결된 BOM이 없습니다. productCode={}", productCode);
            return false;
        }

        Map<Long, RequiredMaterial> requiredByMaterialId = new LinkedHashMap<>();
        for (Bom bom : boms) {
            bom.getBomItems().stream()
                    .filter(bomItem -> bomItem.getMaterial() != null)
                    .filter(bomItem -> bomItem.getMaterial().getId() != null)
                    .filter(bomItem -> bomItem.getRequiredQuantity() != null)
                    .forEach(bomItem -> requiredByMaterialId.merge(
                            bomItem.getMaterial().getId(),
                            new RequiredMaterial(
                                    bomItem.getMaterial().getId(),
                                    bomItem.getMaterial().getMaterialCode(),
                                    bomItem.getMaterial().getMaterialName(),
                                    bomItem.getRequiredQuantity()
                            ),
                            (left, right) -> left.add(right.requiredQuantity())
                    ));
        }

        if (requiredByMaterialId.isEmpty()) {
            log.warn("[자재 확인] BOM_ITEM이 없어 자재 소요량이 없습니다. productCode={}", productCode);
            return true;
        }

        for (RequiredMaterial requiredMaterial : requiredByMaterialId.values()) {
            BigDecimal availableQuantity = materialLotRepo.sumCurrentQuantityByMaterialId(requiredMaterial.materialId());
            if (availableQuantity.compareTo(requiredMaterial.requiredQuantity()) < 0) {
                log.warn("[자재 부족] {}({}) 현재={}, 필요={}",
                        requiredMaterial.materialName(),
                        requiredMaterial.materialCode(),
                        availableQuantity,
                        requiredMaterial.requiredQuantity());
                return false;
            }
        }
        return true;
    }

    @Transactional
    public void reportProduction(ProductionReportDto dto) {
        Process process = processRepo.findByProcessType(dto.getProcessType())
                .orElseThrow(() -> new RuntimeException("등록되지 않은 공정 타입: " + dto.getProcessType()));
        Equipment equipment = process.getEquipment();

        WorkOrder order = orderRepo.findById(dto.getOrderId())
                .orElseThrow(() -> new RuntimeException("작업 지시를 찾을 수 없습니다. ID: " + dto.getOrderId()));
        if ("COMPLETED".equals(order.getWorkOrderStatus())) {
            return;
        }
        ProductLot productLot = order.getProductLot();

        /* 1~3. 측정값 구성 + MEASUREMENT_SPEC 비교 판정 (첫 위반 항목만 defectType으로 기록) */
        Map<String, BigDecimal> measurements = buildMeasurements(dto);

        String result = "OK";
        DefectType defectType = null;
        for (Map.Entry<String, BigDecimal> entry : measurements.entrySet()) {
            MeasurementSpec spec = measurementSpecRepo.findByMeasurementCode(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("등록되지 않은 측정 스펙: " + entry.getKey()));
            BigDecimal value = entry.getValue();
            boolean outOfRange = value.compareTo(spec.getMinValue()) < 0 || value.compareTo(spec.getMaxValue()) > 0;
            if (outOfRange && "OK".equals(result)) {
                result = "NG";
                defectType = spec.getDefectType();
            }
        }

        QualityInspection inspection = qualityInspectionRepo.save(QualityInspection.builder()
                .productLot(productLot)
                .process(process)
                .equipment(equipment)
                .defectType(defectType)
                .inspectorEmployee(order.getManagerEmployee())
                .inspectionResult(result)
                .inspectionAt(LocalDateTime.now())
                .build());

        for (Map.Entry<String, BigDecimal> entry : measurements.entrySet()) {
            inspectionMeasurementRepo.save(com.mes.backend.entity.InspectionMeasurement.builder()
                    .qualityInspection(inspection)
                    .measurementCode(entry.getKey())
                    .measuredValue(entry.getValue())
                    .build());
        }

        /* 4. 이번 공정에 걸린 BOM 자재만 FIFO로 소진, 부족하면 SHORTAGE로 전체 롤백 */
        List<BomItem> bomItems = bomItemRepo.findAllByBomAndInputProcess(order.getBom(), process);
        for (BomItem bomItem : bomItems) {
            consumeFifo(bomItem, order, process, productLot);
        }

        /* 5. 포장(마지막 공정)일 때만 생산수량 증가 + 완료판정 */
        if (dto.getProcessType() == PROCESS_TYPE_PACKAGING) {
            int newQty = (productLot.getCurrentQty() == null ? 0 : productLot.getCurrentQty()) + 1;
            productLot.setCurrentQty(newQty);
            if (order.getOrderQuantity() != null && newQty >= order.getOrderQuantity()) {
                order.setWorkOrderStatus("COMPLETED");
                order.setCompletedAt(LocalDateTime.now());
                productLot.setLotStatus("생산완료");
            }
        }
    }

    private Map<String, BigDecimal> buildMeasurements(ProductionReportDto dto) {
        Map<String, BigDecimal> measurements = new LinkedHashMap<>();
        if (dto.getProcessType() == PROCESS_TYPE_INSPECTION) {
            measurements.put("VOLTAGE", BigDecimal.valueOf(dto.getVoltageX100()).divide(BigDecimal.valueOf(100)));
            measurements.put("CAPACITY", BigDecimal.valueOf(dto.getCapacityX1000()).divide(BigDecimal.valueOf(1000)));
            measurements.put("RESISTANCE", BigDecimal.valueOf(dto.getResistanceMohm()));
        } else {
            measurements.put(measurementCodeFor(dto.getProcessType()), BigDecimal.valueOf(dto.getValue()));
        }
        return measurements;
    }

    private String measurementCodeFor(int processType) {
        switch (processType) {
            case 49: return "THICKNESS";
            case 50: return "ALIGNMENT";
            case 51: return "CURRENT";
            case 52: return "TORQUE";
            case 54: return "WEIGHT";
            default: throw new IllegalArgumentException("알 수 없는 processType: " + processType);
        }
    }

    private void consumeFifo(BomItem bomItem, WorkOrder order, Process process, ProductLot productLot) {
        BigDecimal remaining = bomItem.getRequiredQuantity();
        List<MaterialLot> lots = materialLotRepo.findAllByMaterialOrderByReceiptDateAsc(bomItem.getMaterial());
        for (MaterialLot lot : lots) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal available = lot.getCurrentQuantity();
            if (available == null || available.signum() <= 0) {
                continue;
            }
            BigDecimal consume = available.min(remaining);
            lot.setCurrentQuantity(available.subtract(consume));
            remaining = remaining.subtract(consume);
            materialTransactionRepo.save(com.mes.backend.entity.MaterialTransaction.builder()
                    .materialLot(lot)
                    .process(process)
                    .productLot(productLot)
                    .employee(order.getManagerEmployee())
                    .transactionType("CONSUME")
                    .quantity(consume)
                    .transactionAt(LocalDateTime.now())
                    .build());
        }
        if (remaining.signum() > 0) {
            throw new CustomException("SHORTAGE", "MATERIAL_SHORTAGE:" + bomItem.getMaterial().getMaterialName());
        }
    }

    private record RequiredMaterial(Long materialId, String materialCode, String materialName, BigDecimal requiredQuantity) {
        private RequiredMaterial add(BigDecimal additionalQuantity) {
            return new RequiredMaterial(materialId, materialCode, materialName, requiredQuantity.add(additionalQuantity));
        }
    }

}
