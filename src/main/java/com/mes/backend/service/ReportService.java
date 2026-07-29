package com.mes.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.report.ReportDailyProductionDto;
import com.mes.backend.dto.report.ReportLotDto;
import com.mes.backend.dto.report.ReportMaterialDto;
import com.mes.backend.dto.report.ReportProcessChartDto;
import com.mes.backend.dto.report.ReportProcessDto;
import com.mes.backend.dto.report.ReportQualityDto;
import com.mes.backend.dto.report.ReportSummaryDto;
import com.mes.backend.entity.Bom;
import com.mes.backend.entity.DefectType;
import com.mes.backend.entity.Employee;
import com.mes.backend.entity.Equipment;
import com.mes.backend.entity.InspectionMeasurement;
import com.mes.backend.entity.Material;
import com.mes.backend.entity.MaterialLot;
import com.mes.backend.entity.MaterialTransaction;
import com.mes.backend.entity.Product;
import com.mes.backend.entity.ProductLot;
import com.mes.backend.entity.QualityInspection;
import com.mes.backend.entity.WorkOrder;
import com.mes.backend.repository.MaterialTransactionRepository;
import com.mes.backend.repository.ProductLotRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter CHART_DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd");
    private static final String INSPECTION_PROCESS_CODE = "PROC-050";
    private static final int TOTAL_PROCESS_COUNT = 6;

    private final ProductLotRepository productLotRepository;
    private final MaterialTransactionRepository materialTransactionRepository;

    @Transactional(readOnly = true)
    public List<ReportLotDto> getLots(
            String startDate,
            String endDate,
            String product,
            String equipment,
            String process,
            String keyword
    ) {
        return getFilteredRows(startDate, endDate, product, equipment, process, keyword);
    }

    @Transactional(readOnly = true)
    public ReportSummaryDto getSummary(
            String startDate,
            String endDate,
            String product,
            String equipment,
            String process,
            String keyword
    ) {
        List<ReportLotDto> rows = getFilteredRows(startDate, endDate, product, equipment, process, keyword);
        int planQty = rows.stream().mapToInt(ReportLotDto::getPlanQty).sum();
        int actualQty = rows.stream().mapToInt(ReportLotDto::getActualQty).sum();
        int goodQty = rows.stream().mapToInt(ReportLotDto::getGoodQty).sum();
        int defectQty = rows.stream().mapToInt(ReportLotDto::getDefectQty).sum();

        return ReportSummaryDto.builder()
                .lotCount(rows.size())
                .planQty(planQty)
                .actualQty(actualQty)
                .goodQty(goodQty)
                .defectQty(defectQty)
                .achievementRate(rate(actualQty, planQty))
                .yieldRate(rate(goodQty, goodQty + defectQty))
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReportDailyProductionDto> getDailyProduction(
            String startDate,
            String endDate,
            String product,
            String equipment,
            String process,
            String keyword
    ) {
        return getFilteredRows(startDate, endDate, product, equipment, process, keyword).stream()
                .filter(row -> !isBlank(row.getProductionDate()))
                .collect(Collectors.groupingBy(
                        ReportLotDto::getProductionDate,
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> ReportDailyProductionDto.builder()
                        .date(CHART_DATE_FORMAT.format(LocalDate.parse(entry.getKey())))
                        .plan(entry.getValue().stream().mapToInt(ReportLotDto::getPlanQty).sum())
                        .actual(entry.getValue().stream().mapToInt(ReportLotDto::getActualQty).sum())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportProcessChartDto> getProcessProduction(
            String startDate,
            String endDate,
            String product,
            String equipment,
            String process,
            String keyword
    ) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        String normalizedProduct = normalize(product);
        String normalizedEquipment = normalize(equipment);
        String normalizedProcess = normalize(process);
        String normalizedKeyword = normalize(keyword);
        Map<Long, List<MaterialTransaction>> materialTransactionsByLotId = getMaterialTransactionsByLotId();

        return productLotRepository.findAllForReportPage().stream()
                .filter(lot -> matchesLotFilters(lot, materialTransactionsByLotId.getOrDefault(lot.getId(), List.of()),
                        start, end, normalizedProduct, normalizedEquipment, normalizedProcess, normalizedKeyword))
                .flatMap(lot -> lot.getQualityInspections().stream())
                .filter(inspection -> inspection.getProcess() != null)
                .collect(Collectors.groupingBy(
                        inspection -> inspection.getProcess().getProcessCode(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .sorted(Comparator.comparingInt(entry -> processSequence(entry.getValue().get(0))))
                .map(entry -> ReportProcessChartDto.builder()
                        .process(processLabel(entry.getValue().get(0)))
                        .output(entry.getValue().size())
                        .defect(entry.getValue().stream().filter(this::isNg).count())
                        .build())
                .toList();
    }

    private List<ReportLotDto> getFilteredRows(
            String startDate,
            String endDate,
            String product,
            String equipment,
            String process,
            String keyword
    ) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        String normalizedProduct = normalize(product);
        String normalizedEquipment = normalize(equipment);
        String normalizedProcess = normalize(process);
        String normalizedKeyword = normalize(keyword);
        Map<Long, List<MaterialTransaction>> materialTransactionsByLotId = getMaterialTransactionsByLotId();

        return productLotRepository.findAllForReportPage().stream()
                .filter(lot -> matchesLotFilters(lot, materialTransactionsByLotId.getOrDefault(lot.getId(), List.of()),
                        start, end, normalizedProduct, normalizedEquipment, normalizedProcess, normalizedKeyword))
                .map(lot -> toLotDto(lot, materialTransactionsByLotId.getOrDefault(lot.getId(), List.of())))
                .sorted(Comparator
                        .comparing(ReportLotDto::getProductionDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ReportLotDto::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private boolean matchesLotFilters(
            ProductLot lot,
            List<MaterialTransaction> materialTransactions,
            LocalDate start,
            LocalDate end,
            String normalizedProduct,
            String normalizedEquipment,
            String normalizedProcess,
            String normalizedKeyword
    ) {
        LocalDate productionDate = productionDateOf(lot);
        return matchesStartDate(productionDate, start)
                && matchesEndDate(productionDate, end)
                && matchesProduct(lot, normalizedProduct)
                && matchesEquipment(lot, normalizedEquipment)
                && matchesProcess(lot, normalizedProcess)
                && matchesKeyword(lot, materialTransactions, normalizedKeyword);
    }

    private ReportLotDto toLotDto(ProductLot lot, List<MaterialTransaction> materialTransactions) {
        WorkOrder workOrder = lot.getWorkOrder();
        Bom bom = workOrder != null ? workOrder.getBom() : null;
        Product product = bom != null ? bom.getProduct() : null;
        List<QualityInspection> inspections = sortedInspections(lot);
        int planQty = workOrder != null && workOrder.getOrderQuantity() != null ? workOrder.getOrderQuantity() : 0;
        int actualQty = lot.getCurrentQty() != null ? lot.getCurrentQty() : 0;
        /* goodQty/defectQty/yieldRate는 유닛(unitSequence) 단위 최종 판정 기준: 유닛 하나가
         * 거친 6개 공정 판정이 전부 모였을 때, 하나라도 NG면 그 유닛은 최종 NG. 아직 6개가
         * 안 모인 유닛(진행 중)은 조용히 제외한다. */
        Collection<List<QualityInspection>> completedUnits = completedUnitGroups(inspections);
        int completedUnitQty = completedUnits.size();
        int defectQty = (int) completedUnits.stream().filter(this::isUnitNg).count();
        int goodQty = completedUnitQty - defectQty;

        return ReportLotDto.builder()
                .id(lot.getId())
                .productionDate(formatDate(productionDateOf(lot)))
                .lotNo(valueOrEmpty(lot.getProductLotNo()))
                .productCode(product != null ? valueOrEmpty(product.getProductCode()) : "")
                .productName(product != null ? valueOrEmpty(product.getProductName()) : "")
                .workOrderNo(workOrder != null ? valueOrEmpty(workOrder.getWorkOrderNo()) : "")
                .planQty(planQty)
                .actualQty(actualQty)
                .goodQty(goodQty)
                .defectQty(defectQty)
                .status(statusOf(lot, workOrder))
                .equipment(equipmentName(latestInspection(inspections).orElse(null)))
                .yieldRate(rate(goodQty, completedUnitQty))
                .processes(toProcessDtos(inspections))
                .materials(toMaterialDtos(materialTransactions))
                .quality(toQualityDto(inspections, defectQty))
                .build();
    }

    private List<ReportProcessDto> toProcessDtos(List<QualityInspection> inspections) {
        return inspections.stream()
                .filter(inspection -> inspection.getProcess() != null)
                .collect(Collectors.groupingBy(
                        inspection -> inspection.getProcess().getProcessCode(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
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
                    material.getUnit()
            );
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

    private ReportQualityDto toQualityDto(List<QualityInspection> inspections, int defectQty) {
        QualityInspection measurementSource = representativeQualityInspection(inspections).orElse(null);
        QualityInspection infoSource = defectQty > 0
                ? latestNgInspection(inspections).orElse(measurementSource)
                : measurementSource;
        DefectType defectType = infoSource != null ? infoSource.getDefectType() : null;

        return ReportQualityDto.builder()
                .result(defectQty > 0 ? "NG" : resultOf(infoSource))
                .inspectedAt(formatDateTime(infoSource != null ? infoSource.getInspectionAt() : null))
                .inspectorName(workerName(infoSource))
                .defectCode(defectType != null ? valueOrEmpty(defectType.getDefectCode()) : "")
                .defectType(defectType != null ? defectName(defectType) : "")
                .defectQty(defectQty)
                .voltage(findMeasuredValue(measurementSource, "VOLTAGE"))
                .resistance(findMeasuredValue(measurementSource, "RESISTANCE"))
                .build();
    }

    private Optional<QualityInspection> representativeQualityInspection(List<QualityInspection> inspections) {
        return inspections.stream()
                .filter(inspection -> inspection.getProcess() != null
                        && INSPECTION_PROCESS_CODE.equals(inspection.getProcess().getProcessCode()))
                .max(Comparator.comparing(QualityInspection::getInspectionAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .or(() -> latestInspection(inspections));
    }

    private Optional<QualityInspection> latestNgInspection(List<QualityInspection> inspections) {
        return inspections.stream()
                .filter(this::isNg)
                .max(Comparator.comparing(QualityInspection::getInspectionAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private Map<Long, List<MaterialTransaction>> getMaterialTransactionsByLotId() {
        List<ProductLot> lots = productLotRepository.findAll();
        List<Long> lotIds = lots.stream()
                .map(ProductLot::getId)
                .filter(Objects::nonNull)
                .toList();
        if (lotIds.isEmpty()) {
            return Map.of();
        }
        return materialTransactionRepository.findConsumeTransactionsForProductLots(lotIds).stream()
                .filter(transaction -> transaction.getProductLot() != null)
                .collect(Collectors.groupingBy(transaction -> transaction.getProductLot().getId(), LinkedHashMap::new, Collectors.toList()));
    }

    private List<QualityInspection> sortedInspections(ProductLot lot) {
        if (lot.getQualityInspections() == null) {
            return List.of();
        }
        return lot.getQualityInspections().stream()
                .sorted(Comparator
                        .comparing((QualityInspection inspection) -> processSequence(inspection), Comparator.naturalOrder())
                        .thenComparing(QualityInspection::getInspectionAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(QualityInspection::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private Optional<QualityInspection> latestInspection(List<QualityInspection> inspections) {
        return inspections.stream()
                .max(Comparator.comparing(QualityInspection::getInspectionAt, Comparator.nullsLast(Comparator.naturalOrder())));
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

    private LocalDate productionDateOf(ProductLot lot) {
        WorkOrder workOrder = lot.getWorkOrder();
        if (workOrder != null && workOrder.getCompletedAt() != null) {
            return workOrder.getCompletedAt().toLocalDate();
        }
        LocalDateTime latestInspectionAt = latestInspection(sortedInspections(lot))
                .map(QualityInspection::getInspectionAt)
                .orElse(null);
        if (latestInspectionAt != null) {
            return latestInspectionAt.toLocalDate();
        }
        if (lot.getLotCreatedAt() != null) {
            return lot.getLotCreatedAt().toLocalDate();
        }
        if (workOrder != null && workOrder.getActualStartAt() != null) {
            return workOrder.getActualStartAt().toLocalDate();
        }
        return workOrder != null ? workOrder.getDueDate() : null;
    }

    private boolean matchesStartDate(LocalDate productionDate, LocalDate start) {
        return start == null || (productionDate != null && !productionDate.isBefore(start));
    }

    private boolean matchesEndDate(LocalDate productionDate, LocalDate end) {
        return end == null || (productionDate != null && !productionDate.isAfter(end));
    }

    private boolean matchesProduct(ProductLot lot, String normalizedProduct) {
        if (normalizedProduct == null) {
            return true;
        }
        Product product = productOf(lot);
        return product != null && normalizedProduct.equals(normalize(product.getProductCode()));
    }

    private boolean matchesEquipment(ProductLot lot, String normalizedEquipment) {
        return normalizedEquipment == null || lot.getQualityInspections().stream()
                .map(QualityInspection::getEquipment)
                .filter(Objects::nonNull)
                .map(Equipment::getEquipmentName)
                .map(this::normalize)
                .anyMatch(normalizedEquipment::equals);
    }

    private boolean matchesProcess(ProductLot lot, String normalizedProcess) {
        return normalizedProcess == null || lot.getQualityInspections().stream()
                .map(QualityInspection::getProcess)
                .filter(Objects::nonNull)
                .map(com.mes.backend.entity.Process::getProcessCode)
                .map(this::normalize)
                .anyMatch(normalizedProcess::equals);
    }

    private boolean matchesKeyword(ProductLot lot, List<MaterialTransaction> materialTransactions, String normalizedKeyword) {
        if (normalizedKeyword == null) {
            return true;
        }
        Product product = productOf(lot);
        WorkOrder workOrder = lot.getWorkOrder();
        List<String> searchableValues = new ArrayList<>();
        searchableValues.add(lot.getProductLotNo());
        searchableValues.add(product != null ? product.getProductCode() : null);
        searchableValues.add(product != null ? product.getProductName() : null);
        searchableValues.add(workOrder != null ? workOrder.getWorkOrderNo() : null);
        latestInspection(sortedInspections(lot)).map(QualityInspection::getEquipment).map(Equipment::getEquipmentName)
                .ifPresent(searchableValues::add);

        for (MaterialTransaction transaction : materialTransactions) {
            MaterialLot materialLot = transaction.getMaterialLot();
            Material material = materialLot != null ? materialLot.getMaterial() : null;
            searchableValues.add(material != null ? material.getMaterialCode() : null);
            searchableValues.add(material != null ? material.getMaterialName() : null);
            searchableValues.add(materialLot != null ? materialLot.getMaterialLotNo() : null);
        }

        return searchableValues.stream()
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(Objects::nonNull)
                .anyMatch(value -> value.contains(normalizedKeyword));
    }

    private Product productOf(ProductLot lot) {
        WorkOrder workOrder = lot.getWorkOrder();
        Bom bom = workOrder != null ? workOrder.getBom() : null;
        return bom != null ? bom.getProduct() : null;
    }

    private String statusOf(ProductLot lot, WorkOrder workOrder) {
        if (workOrder != null && !isBlank(workOrder.getWorkOrderStatus())) {
            return workOrder.getWorkOrderStatus();
        }
        return valueOrEmpty(lot.getLotStatus());
    }

    private String resultOf(QualityInspection inspection) {
        return inspection != null && !isBlank(inspection.getInspectionResult()) ? inspection.getInspectionResult() : "-";
    }

    private String equipmentName(QualityInspection inspection) {
        Equipment equipment = inspection != null ? inspection.getEquipment() : null;
        return equipment != null ? valueOrEmpty(equipment.getEquipmentName()) : "";
    }

    private String workerName(QualityInspection inspection) {
        Employee inspector = inspection != null ? inspection.getInspectorEmployee() : null;
        if (inspector != null) {
            return valueOrEmpty(inspector.getEmployeeName());
        }
        if (inspection != null && inspection.getProcess() != null && inspection.getProcess().getManagerEmployee() != null) {
            return valueOrEmpty(inspection.getProcess().getManagerEmployee().getEmployeeName());
        }
        return "";
    }

    private BigDecimal findMeasuredValue(QualityInspection inspection, String measurementCode) {
        if (inspection == null || inspection.getInspectionMeasurements() == null) {
            return null;
        }
        return inspection.getInspectionMeasurements().stream()
                .filter(measurement -> measurementCode.equals(measurement.getMeasurementCode()))
                .map(InspectionMeasurement::getMeasuredValue)
                .findFirst()
                .orElse(null);
    }

    private int processSequence(QualityInspection inspection) {
        if (inspection == null || inspection.getProcess() == null || inspection.getProcess().getSequenceNo() == null) {
            return Integer.MAX_VALUE;
        }
        return inspection.getProcess().getSequenceNo();
    }

    private String processLabel(QualityInspection inspection) {
        if (inspection == null || inspection.getProcess() == null) {
            return "";
        }
        String processName = inspection.getProcess().getProcessName();
        if (isBlank(processName)) {
            return valueOrEmpty(inspection.getProcess().getProcessCode());
        }
        return processName.replace("공정", "");
    }

    private boolean isNg(QualityInspection inspection) {
        return "NG".equalsIgnoreCase(inspection.getInspectionResult());
    }

    /* unitSequence로 그룹핑해서, 6개 공정(TOTAL_PROCESS_COUNT) 판정이 전부 모인 유닛만
     * "완성된 유닛"으로 취급한다. 아직 6개가 안 모인 유닛(진행 중)은 조용히 제외한다. */
    private Collection<List<QualityInspection>> completedUnitGroups(List<QualityInspection> inspections) {
        return inspections.stream()
                .filter(inspection -> inspection.getUnitSequence() != null)
                .collect(Collectors.groupingBy(QualityInspection::getUnitSequence))
                .values().stream()
                .filter(group -> group.size() == TOTAL_PROCESS_COUNT)
                .toList();
    }

    private boolean isUnitNg(List<QualityInspection> unitInspections) {
        return unitInspections.stream().anyMatch(this::isNg);
    }

    private double rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private BigDecimal safeQuantity(BigDecimal quantity) {
        return quantity != null ? quantity : BigDecimal.ZERO;
    }

    private String defectName(DefectType defectType) {
        return defectType.getDefectName() != null ? defectType.getDefectName() : defectType.getDefectCode();
    }

    private String formatDate(LocalDate value) {
        return value != null ? DATE_FORMAT.format(value) : "";
    }

    private String formatDateTime(LocalDateTime value) {
        return value != null ? DATE_TIME_FORMAT.format(value) : "";
    }

    private LocalDate parseDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date format must be yyyy-MM-dd: " + value);
        }
    }

    private String normalize(String value) {
        return isBlank(value) ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String valueOrEmpty(String value) {
        return value != null ? value : "";
    }

    private record MaterialLotKey(String materialCode, String materialName, String materialLotNo, String unit) {
    }
}
