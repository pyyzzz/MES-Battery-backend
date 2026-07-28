// 완제품LOT(ProductLot) 조회 전용 서비스 - assignWorkToMachine()에서 자동 생성되므로 쓰기 API 없음
package com.mes.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.ProductLotDetailDto;
import com.mes.backend.dto.ProductLotSummaryDto;
import com.mes.backend.dto.report.ReportMaterialDto;
import com.mes.backend.dto.report.ReportProcessDto;
import com.mes.backend.entity.Bom;
import com.mes.backend.entity.Employee;
import com.mes.backend.entity.Equipment;
import com.mes.backend.entity.Material;
import com.mes.backend.entity.MaterialLot;
import com.mes.backend.entity.MaterialTransaction;
import com.mes.backend.entity.Product;
import com.mes.backend.entity.ProductLot;
import com.mes.backend.entity.QualityInspection;
import com.mes.backend.entity.WorkOrder;
import com.mes.backend.repository.MaterialTransactionRepository;
import com.mes.backend.repository.ProductLotRepository;
import com.mes.backend.repository.QualityInspectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductLotService {

    private static final String INSPECTION_RESULT_OK = "OK";
    private static final String INSPECTION_RESULT_NG = "NG";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ProductLotRepository productLotRepo;
    private final QualityInspectionRepository qualityInspectionRepo;
    private final MaterialTransactionRepository materialTransactionRepo;

    @Transactional(readOnly = true)
    public List<ProductLotSummaryDto> search(String productLotNo, String productName, String workOrderNo, String lotStatus) {
        return productLotRepo.search(productLotNo, productName, workOrderNo, lotStatus).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductLotDetailDto getDetail(Long id) {
        ProductLot productLot = productLotRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("완제품LOT을 찾을 수 없습니다. ID: " + id));

        long inspectionCount = qualityInspectionRepo.countByProductLot_Id(id);
        long passCount = qualityInspectionRepo.countByProductLot_IdAndInspectionResult(id, INSPECTION_RESULT_OK);
        long failCount = qualityInspectionRepo.countByProductLot_IdAndInspectionResult(id, INSPECTION_RESULT_NG);
        List<MaterialTransaction> consumeTransactions = materialTransactionRepo.findConsumeTransactionsForProductLots(List.of(id));

        return ProductLotDetailDto.builder()
                .id(productLot.getId())
                .productLotNo(productLot.getProductLotNo())
                .productName(productNameOf(productLot))
                .workOrderNo(workOrderNoOf(productLot))
                .currentQty(productLot.getCurrentQty())
                .lotStatus(productLot.getLotStatus())
                .lotCreatedAt(productLot.getLotCreatedAt())
                .inspectionCount(inspectionCount)
                .passCount(passCount)
                .failCount(failCount)
                .processes(toProcessDtos(productLot))
                .materials(toMaterialDtos(consumeTransactions))
                .build();
    }

    /* 공정코드별로 검사이력을 묶어서 공정 하나당 카드 하나(장비/작업자는 최신 이력 기준, 결과는 NG 건수 요약) */
    private List<ReportProcessDto> toProcessDtos(ProductLot productLot) {
        if (productLot.getQualityInspections() == null) {
            return List.of();
        }
        return productLot.getQualityInspections().stream()
                .filter(inspection -> inspection.getProcess() != null)
                .collect(Collectors.groupingBy(
                        inspection -> inspection.getProcess().getProcessCode(),
                        LinkedHashMap::new,
                        Collectors.toList()))
                .values()
                .stream()
                .sorted(Comparator.comparingInt(group -> processSequence(group.get(0))))
                .map(this::toProcessDto)
                .toList();
    }

    private ReportProcessDto toProcessDto(List<QualityInspection> inspections) {
        QualityInspection first = inspections.get(0);
        QualityInspection latest = inspections.stream()
                .max(Comparator.comparing(QualityInspection::getInspectionAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(first);
        long defectCount = inspections.stream().filter(this::isNg).count();

        return ReportProcessDto.builder()
                .processCode(first.getProcess() != null ? valueOrEmpty(first.getProcess().getProcessCode()) : "")
                .processName(first.getProcess() != null ? valueOrEmpty(first.getProcess().getProcessName()) : "")
                .equipmentName(equipmentName(latest))
                .workerName(workerName(latest))
                .startedAt(formatDateTime(minInspectionAt(inspections)))
                .endedAt(formatDateTime(maxInspectionAt(inspections)))
                .result(defectCount > 0 ? "NG " + defectCount + "건" : "완료")
                .build();
    }

    private List<ReportMaterialDto> toMaterialDtos(List<MaterialTransaction> transactions) {
        Map<MaterialLotKey, BigDecimal> quantitiesByMaterialLot = new LinkedHashMap<>();

        for (MaterialTransaction transaction : transactions) {
            MaterialLot materialLot = transaction.getMaterialLot();
            if (materialLot == null || materialLot.getMaterial() == null) {
                continue;
            }
            Material material = materialLot.getMaterial();
            MaterialLotKey key = new MaterialLotKey(
                    material.getMaterialCode(),
                    material.getMaterialName(),
                    materialLot.getMaterialLotNo(),
                    material.getUnit());
            quantitiesByMaterialLot.merge(key, safeQuantity(transaction.getQuantity()), BigDecimal::add);
        }

        return quantitiesByMaterialLot.entrySet().stream()
                .map(entry -> ReportMaterialDto.builder()
                        .materialCode(valueOrEmpty(entry.getKey().materialCode()))
                        .materialName(valueOrEmpty(entry.getKey().materialName()))
                        .materialLotNo(valueOrEmpty(entry.getKey().materialLotNo()))
                        .quantity(entry.getValue())
                        .unit(valueOrEmpty(entry.getKey().unit()))
                        .build())
                .toList();
    }

    private int processSequence(QualityInspection inspection) {
        if (inspection == null || inspection.getProcess() == null || inspection.getProcess().getSequenceNo() == null) {
            return Integer.MAX_VALUE;
        }
        return inspection.getProcess().getSequenceNo();
    }

    private String equipmentName(QualityInspection inspection) {
        Equipment equipment = inspection != null ? inspection.getEquipment() : null;
        return equipment != null ? valueOrEmpty(equipment.getEquipmentName()) : "";
    }

    private String workerName(QualityInspection inspection) {
        Employee inspector = inspection != null ? inspection.getInspectorEmployee() : null;
        return inspector != null ? valueOrEmpty(inspector.getEmployeeName()) : "";
    }

    private LocalDateTime minInspectionAt(List<QualityInspection> inspections) {
        return inspections.stream()
                .map(QualityInspection::getInspectionAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private LocalDateTime maxInspectionAt(List<QualityInspection> inspections) {
        return inspections.stream()
                .map(QualityInspection::getInspectionAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private boolean isNg(QualityInspection inspection) {
        return "NG".equalsIgnoreCase(inspection.getInspectionResult());
    }

    private BigDecimal safeQuantity(BigDecimal quantity) {
        return quantity != null ? quantity : BigDecimal.ZERO;
    }

    private String formatDateTime(LocalDateTime value) {
        return value != null ? DATE_TIME_FORMAT.format(value) : "";
    }

    private String valueOrEmpty(String value) {
        return value != null ? value : "";
    }

    private record MaterialLotKey(String materialCode, String materialName, String materialLotNo, String unit) {
    }

    private ProductLotSummaryDto toSummaryDto(ProductLot productLot) {
        return ProductLotSummaryDto.builder()
                .id(productLot.getId())
                .productLotNo(productLot.getProductLotNo())
                .productName(productNameOf(productLot))
                .workOrderNo(workOrderNoOf(productLot))
                .currentQty(productLot.getCurrentQty())
                .lotStatus(productLot.getLotStatus())
                .lotCreatedAt(productLot.getLotCreatedAt())
                .build();
    }

    private String productNameOf(ProductLot productLot) {
        WorkOrder workOrder = productLot.getWorkOrder();
        Bom bom = workOrder != null ? workOrder.getBom() : null;
        Product product = bom != null ? bom.getProduct() : null;
        return product != null ? product.getProductName() : null;
    }

    private String workOrderNoOf(ProductLot productLot) {
        WorkOrder workOrder = productLot.getWorkOrder();
        return workOrder != null ? workOrder.getWorkOrderNo() : null;
    }
}
