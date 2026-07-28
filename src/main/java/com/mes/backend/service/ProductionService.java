package com.mes.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.mes.backend.dto.ProductionReportDto;
import com.mes.backend.entity.Bom;
import com.mes.backend.entity.BomItem;
import com.mes.backend.entity.DefectType;
import com.mes.backend.entity.Equipment;
import com.mes.backend.entity.MaterialLot;
import com.mes.backend.entity.MaterialTransaction;
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
    private static final int MAX_LOCK_RETRIES = 3;
    private static final long LOCK_RETRY_DELAY_MS = 300;

    private final PlatformTransactionManager transactionManager;
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

    /* 라인이 1개뿐이라 시스템 전체에 동시 진행 중인 작업지시는 최대 1개로 가정(machineId별 할당 개념 없음) */
    @Transactional
    public WorkOrder assignWorkToMachine(String machineId) {
        WorkOrder order = orderRepo.findFirstByWorkOrderStatus("IN_PROGRESS")
                .orElseGet(() -> orderRepo.findFirstByWorkOrderStatusOrderByIdAsc("WAITING")
                        .map(waiting -> {
                            String productCode = waiting.getBom() != null && waiting.getBom().getProduct() != null
                                    ? waiting.getBom().getProduct().getProductCode() : null;
                            if (!isMaterialAvailableForOrder(waiting)) {
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

    private boolean isMaterialAvailableForOrder(WorkOrder order) {
        if (order == null || order.getBom() == null) {
            log.warn("[자재 확인 실패] 작업지시에 BOM이 없습니다. workOrderId={}", order != null ? order.getId() : null);
            return false;
        }

        int orderQuantity = order.getOrderQuantity() != null ? order.getOrderQuantity() : 0;
        if (orderQuantity <= 0) {
            log.warn("[자재 확인 실패] 작업지시 수량이 올바르지 않습니다. workOrderId={}, orderQuantity={}",
                    order.getId(),
                    order.getOrderQuantity());
            return false;
        }

        Map<Long, RequiredMaterial> requiredByMaterialId = new LinkedHashMap<>();
        for (BomItem bomItem : bomItemRepo.findAllByBom(order.getBom())) {
            mergeRequiredMaterial(requiredByMaterialId, bomItem, orderQuantity);
        }

        if (requiredByMaterialId.isEmpty()) {
            log.warn("[자재 확인] BOM_ITEM이 없어 자재 소요량이 없습니다. workOrderId={}", order.getId());
            return false;
        }

        for (RequiredMaterial requiredMaterial : requiredByMaterialId.values()) {
            BigDecimal availableQuantity = materialLotRepo.sumCurrentQuantityByMaterialId(requiredMaterial.materialId());
            if (availableQuantity.compareTo(requiredMaterial.requiredQuantity()) < 0) {
                log.warn("[자재 부족] {}({}) 현재={}, 필요={}, workOrderId={}",
                        requiredMaterial.materialName(),
                        requiredMaterial.materialCode(),
                        availableQuantity,
                        requiredMaterial.requiredQuantity(),
                        order.getId());
                return false;
            }
        }
        return true;
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

    /* 데드락(CannotAcquireLockException)/낙관적 락 충돌(ObjectOptimisticLockingFailureException) 발생 시에만
     * 짧은 대기 후 최대 MAX_LOCK_RETRIES회 재시도. 둘 다 ConcurrencyFailureException의 하위타입이라 한 곳에서 잡음.
     * 매 시도마다 새 트랜잭션이 열려야 하므로(자기 자신을 @Transactional로 재호출하면 프록시를
     * 안 타서 새 트랜잭션이 안 열림) TransactionTemplate으로 직접 트랜잭션 경계를 관리한다.
     * SHORTAGE(CustomException) 등 다른 예외는 ConcurrencyFailureException 계열이 아니라 재시도 대상이 아님. */
    public void reportProduction(ProductionReportDto dto) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        for (int attempt = 1; attempt <= MAX_LOCK_RETRIES; attempt++) {
            try {
                transactionTemplate.executeWithoutResult(status -> reportProductionInternal(dto));
                return;
            } catch (ConcurrencyFailureException e) {
                if (attempt >= MAX_LOCK_RETRIES) {
                    throw e;
                }
                log.warn("[재시도] 동시성 충돌(데드락 또는 낙관적 락 실패로 추정) - {}/{}회, orderId={}, processType={}",
                        attempt, MAX_LOCK_RETRIES, dto.getOrderId(), dto.getProcessType());
                try {
                    Thread.sleep(LOCK_RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    private void reportProductionInternal(ProductionReportDto dto) {
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

        /* 이 LOT+이 공정에서 몇 번째 리포트인지(1부터) - 공정별 큐가 FIFO라서 같은 순번끼리는
         * 같은 물리적 유닛으로 취급 가능(유닛 단위 최종 판정에 사용, 유닛 ID 자체는 아님) */
        int unitSequence = (int) qualityInspectionRepo.countByProductLot_IdAndProcess_Id(productLot.getId(), process.getId()) + 1;

        QualityInspection inspection = qualityInspectionRepo.save(QualityInspection.builder()
                .productLot(productLot)
                .process(process)
                .equipment(equipment)
                .defectType(defectType)
                .inspectorEmployee(order.getManagerEmployee())
                .inspectionResult(result)
                .inspectionAt(LocalDateTime.now())
                .unitSequence(unitSequence)
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
            validateConsumedMaterialsForOutput(order, productLot, newQty);
            productLot.setCurrentQty(newQty);
            if (order.getOrderQuantity() != null && newQty >= order.getOrderQuantity()) {
                order.setWorkOrderStatus("COMPLETED");
                order.setCompletedAt(LocalDateTime.now());
                productLot.setLotStatus("생산완료");
            }
        }
    }

    private void validateConsumedMaterialsForOutput(WorkOrder order, ProductLot productLot, int outputQty) {
        if (order == null || order.getBom() == null) {
            throw new CustomException("SHORTAGE", "MATERIAL_SHORTAGE:BOM");
        }

        Map<Long, RequiredMaterial> requiredByMaterialId = new LinkedHashMap<>();
        for (BomItem bomItem : bomItemRepo.findAllByBom(order.getBom())) {
            mergeRequiredMaterial(requiredByMaterialId, bomItem, outputQty);
        }

        if (requiredByMaterialId.isEmpty()) {
            throw new CustomException("INVALID_BOM", "선택한 제품에 등록된 BOM 자재가 없어 생산할 수 없습니다.");
        }

        Map<Long, BigDecimal> consumedByMaterialId = new LinkedHashMap<>();
        List<MaterialTransaction> transactions =
                materialTransactionRepo.findConsumeTransactionsForProductLots(List.of(productLot.getId()));
        for (MaterialTransaction transaction : transactions) {
            if (transaction.getMaterialLot() == null || transaction.getMaterialLot().getMaterial() == null) {
                continue;
            }
            Long materialId = transaction.getMaterialLot().getMaterial().getId();
            BigDecimal quantity = transaction.getQuantity() != null ? transaction.getQuantity() : BigDecimal.ZERO;
            consumedByMaterialId.merge(materialId, quantity, BigDecimal::add);
        }

        for (RequiredMaterial requiredMaterial : requiredByMaterialId.values()) {
            BigDecimal consumedQuantity = consumedByMaterialId.getOrDefault(requiredMaterial.materialId(), BigDecimal.ZERO);
            if (consumedQuantity.compareTo(requiredMaterial.requiredQuantity()) < 0) {
                log.warn("[생산 보류] 자재 투입 이력 부족. {}({}) 투입={}, 필요={}, productLotId={}",
                        requiredMaterial.materialName(),
                        requiredMaterial.materialCode(),
                        consumedQuantity,
                        requiredMaterial.requiredQuantity(),
                        productLot.getId());
                throw new CustomException("SHORTAGE", "MATERIAL_SHORTAGE:" + requiredMaterial.materialName());
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
            BigDecimal afterQuantity = available.subtract(consume);
            lot.setCurrentQuantity(afterQuantity);
            lot.setLotStatus(afterQuantity.signum() <= 0 ? "DEFECT" : "IN_USE");
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

    private void mergeRequiredMaterial(Map<Long, RequiredMaterial> requiredByMaterialId, BomItem bomItem, int outputQty) {
        if (bomItem.getMaterial() == null
                || bomItem.getMaterial().getId() == null
                || bomItem.getRequiredQuantity() == null) {
            return;
        }

        BigDecimal requiredQuantity = bomItem.getRequiredQuantity().multiply(BigDecimal.valueOf(outputQty));
        requiredByMaterialId.merge(
                bomItem.getMaterial().getId(),
                new RequiredMaterial(
                        bomItem.getMaterial().getId(),
                        bomItem.getMaterial().getMaterialCode(),
                        bomItem.getMaterial().getMaterialName(),
                        requiredQuantity
                ),
                (left, right) -> left.add(right.requiredQuantity())
        );
    }

    private record RequiredMaterial(Long materialId, String materialCode, String materialName, BigDecimal requiredQuantity) {
        private RequiredMaterial add(BigDecimal additionalQuantity) {
            return new RequiredMaterial(materialId, materialCode, materialName, requiredQuantity.add(additionalQuantity));
        }
    }

}
