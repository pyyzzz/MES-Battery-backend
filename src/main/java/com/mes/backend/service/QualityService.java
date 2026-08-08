package com.mes.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.quality.QualityDefectDistributionDto;
import com.mes.backend.dto.quality.QualityInspectionDto;
import com.mes.backend.dto.quality.QualityMeasurementDto;
import com.mes.backend.dto.quality.QualitySummaryDto;
import com.mes.backend.dto.quality.QualityTrendDto;
import com.mes.backend.entity.Bom;
import com.mes.backend.entity.DefectType;
import com.mes.backend.entity.Employee;
import com.mes.backend.entity.Equipment;
import com.mes.backend.entity.InspectionMeasurement;
import com.mes.backend.entity.MeasurementSpec;
import com.mes.backend.entity.Product;
import com.mes.backend.entity.ProductLot;
import com.mes.backend.entity.QualityInspection;
import com.mes.backend.entity.WorkOrder;
import com.mes.backend.repository.MeasurementSpecRepository;
import com.mes.backend.repository.QualityInspectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QualityService {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TREND_DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd");

    private final QualityInspectionRepository qualityInspectionRepository;
    private final MeasurementSpecRepository measurementSpecRepository;

    @Transactional(readOnly = true)
    public List<QualityInspectionDto> getInspections(
            String startDate,
            String endDate,
            String result,
            String defectType,
            String keyword
    ) {
        Map<String, String> unitsByMeasurementCode = getUnitsByMeasurementCode();
        return getFilteredInspections(startDate, endDate, result, defectType, keyword).stream()
                .map(inspection -> toInspectionDto(inspection, unitsByMeasurementCode))
                .toList();
    }

    @Transactional(readOnly = true)
    public QualitySummaryDto getSummary(String startDate, String endDate, String result, String defectType, String keyword) {
        List<QualityInspection> inspections = getFilteredInspections(startDate, endDate, result, defectType, keyword);
        long total = inspections.size();
        long ok = inspections.stream().filter(this::isOk).count();
        long ng = inspections.stream().filter(this::isNg).count();
        int okRate = total > 0 ? (int) Math.round((ok * 100.0) / total) : 0;
        String topDefect = inspections.stream()
                .filter(this::isNg)
                .map(QualityInspection::getDefectType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(this::defectName, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .orElse("-");

        return QualitySummaryDto.builder()
                .total(total)
                .ok(ok)
                .ng(ng)
                .okRate(okRate)
                .topDefect(topDefect)
                .build();
    }

    @Transactional(readOnly = true)
    public List<QualityTrendDto> getTrend(String startDate, String endDate, String result, String defectType, String keyword) {
        return getFilteredInspections(startDate, endDate, result, defectType, keyword).stream()
                .filter(inspection -> inspection.getInspectionAt() != null)
                .collect(Collectors.groupingBy(
                        inspection -> inspection.getInspectionAt().toLocalDate(),
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> QualityTrendDto.builder()
                        .date(TREND_DATE_FORMAT.format(entry.getKey()))
                        .ok(entry.getValue().stream().filter(this::isOk).count())
                        .ng(entry.getValue().stream().filter(this::isNg).count())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QualityDefectDistributionDto> getDefectDistribution(
            String startDate,
            String endDate,
            String result,
            String defectType,
            String keyword
    ) {
        return getFilteredInspections(startDate, endDate, result, defectType, keyword).stream()
                .filter(this::isNg)
                .map(QualityInspection::getDefectType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(this::defectName, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> QualityDefectDistributionDto.builder()
                        .name(entry.getKey())
                        .value(entry.getValue())
                        .build())
                .toList();
    }

    private List<QualityInspection> getFilteredInspections(
            String startDate,
            String endDate,
            String result,
            String defectType,
            String keyword
    ) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        String normalizedResult = normalize(result);
        String normalizedDefectType = normalize(defectType);
        String normalizedKeyword = normalize(keyword);

        return qualityInspectionRepository.findAllForQualityPage().stream()
                .filter(inspection -> matchesStartDate(inspection.getInspectionAt(), start))
                .filter(inspection -> matchesEndDate(inspection.getInspectionAt(), end))
                .filter(inspection -> normalizedResult == null
                        || normalizedResult.equals(normalize(inspection.getInspectionResult())))
                .filter(inspection -> normalizedDefectType == null
                        || matchesDefectType(inspection.getDefectType(), normalizedDefectType))
                .filter(inspection -> normalizedKeyword == null
                        || matchesKeyword(inspection, normalizedKeyword))
                .sorted(Comparator
                        .comparing(QualityInspection::getInspectionAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(QualityInspection::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private QualityInspectionDto toInspectionDto(QualityInspection inspection, Map<String, String> unitsByMeasurementCode) {
        ProductLot productLot = inspection.getProductLot();
        WorkOrder workOrder = productLot != null ? productLot.getWorkOrder() : null;
        Bom bom = workOrder != null ? workOrder.getBom() : null;
        Product product = bom != null ? bom.getProduct() : null;
        Equipment equipment = inspection.getEquipment();
        Employee inspector = inspection.getInspectorEmployee();
        DefectType defectType = inspection.getDefectType();

        List<QualityMeasurementDto> measurements = inspection.getInspectionMeasurements().stream()
                .map(measurement -> toMeasurementDto(measurement, unitsByMeasurementCode))
                .toList();

        return QualityInspectionDto.builder()
                .id(inspection.getId())
                .inspectedAt(formatDateTime(inspection.getInspectionAt()))
                .result(inspection.getInspectionResult())
                .defectCode(defectType != null ? defectType.getDefectCode() : "")
                .defectType(defectType != null ? defectName(defectType) : "")
                .defectQty(isNg(inspection) ? 1 : 0)
                .lotNo(productLot != null ? productLot.getProductLotNo() : "")
                .productName(product != null ? product.getProductName() : "")
                .workOrderNo(workOrder != null ? workOrder.getWorkOrderNo() : "")
                .processCode(inspection.getProcess() != null ? inspection.getProcess().getProcessCode() : "")
                .processName(inspection.getProcess() != null ? inspection.getProcess().getProcessName() : "")
                .machineCode(equipment != null ? equipment.getEquipmentCode() : "")
                .machineName(equipment != null ? equipment.getEquipmentName() : "")
                .workerName(inspector != null ? inspector.getEmployeeName() : "")
                .voltage(findMeasuredValue(inspection, "VOLTAGE", equipment != null ? equipment.getLastBatteryVoltage() : null))
                .humidity(equipment != null ? equipment.getCurrentHumidity() : null)
                .measurements(measurements)
                .build();
    }

    private QualityMeasurementDto toMeasurementDto(InspectionMeasurement measurement, Map<String, String> unitsByMeasurementCode) {
        return QualityMeasurementDto.builder()
                .measurementCode(measurement.getMeasurementCode())
                .measuredValue(measurement.getMeasuredValue())
                .unit(unitsByMeasurementCode.get(measurement.getMeasurementCode()))
                .build();
    }

    private BigDecimal findMeasuredValue(QualityInspection inspection, String measurementCode, BigDecimal fallback) {
        return inspection.getInspectionMeasurements().stream()
                .filter(measurement -> measurementCode.equals(measurement.getMeasurementCode()))
                .map(InspectionMeasurement::getMeasuredValue)
                .findFirst()
                .orElse(fallback);
    }

    private Map<String, String> getUnitsByMeasurementCode() {
        return measurementSpecRepository.findAll().stream()
                .collect(Collectors.toMap(
                        MeasurementSpec::getMeasurementCode,
                        MeasurementSpec::getUnit,
                        (left, right) -> left
                ));
    }

    private boolean matchesKeyword(QualityInspection inspection, String normalizedKeyword) {
        ProductLot productLot = inspection.getProductLot();
        WorkOrder workOrder = productLot != null ? productLot.getWorkOrder() : null;
        Bom bom = workOrder != null ? workOrder.getBom() : null;
        Product product = bom != null ? bom.getProduct() : null;
        Equipment equipment = inspection.getEquipment();
        Employee inspector = inspection.getInspectorEmployee();
        DefectType defectType = inspection.getDefectType();

        return Arrays.asList(
                        productLot != null ? productLot.getProductLotNo() : null,
                        product != null ? product.getProductName() : null,
                        workOrder != null ? workOrder.getWorkOrderNo() : null,
                        inspection.getProcess() != null ? inspection.getProcess().getProcessCode() : null,
                        inspection.getProcess() != null ? inspection.getProcess().getProcessName() : null,
                        equipment != null ? equipment.getEquipmentCode() : null,
                        equipment != null ? equipment.getEquipmentName() : null,
                        inspector != null ? inspector.getEmployeeName() : null,
                        defectType != null ? defectType.getDefectCode() : null,
                        defectType != null ? defectType.getDefectName() : null
                )
                .stream()
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(Objects::nonNull)
                .anyMatch(value -> value.contains(normalizedKeyword));
    }

    private boolean matchesDefectType(DefectType defectType, String normalizedDefectType) {
        if (defectType == null) {
            return false;
        }
        return normalize(defectType.getDefectCode()).equals(normalizedDefectType)
                || normalize(defectType.getDefectName()).equals(normalizedDefectType);
    }

    private boolean matchesStartDate(LocalDateTime inspectionAt, LocalDate start) {
        return start == null || (inspectionAt != null && !inspectionAt.toLocalDate().isBefore(start));
    }

    private boolean matchesEndDate(LocalDateTime inspectionAt, LocalDate end) {
        return end == null || (inspectionAt != null && !inspectionAt.toLocalDate().isAfter(end));
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

    private String formatDateTime(LocalDateTime value) {
        return value != null ? DATE_TIME_FORMAT.format(value) : "";
    }

    private boolean isOk(QualityInspection inspection) {
        return "OK".equalsIgnoreCase(inspection.getInspectionResult());
    }

    private boolean isNg(QualityInspection inspection) {
        return "NG".equalsIgnoreCase(inspection.getInspectionResult());
    }

    private String defectName(DefectType defectType) {
        return defectType.getDefectName() != null ? defectType.getDefectName() : defectType.getDefectCode();
    }

    private String normalize(String value) {
        return isBlank(value) ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
