package com.mes.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.dashboard.DashboardResponseDto;
import com.mes.backend.dto.dashboard.DashboardResponseDto.DashboardDefectTypeDto;
import com.mes.backend.dto.dashboard.DashboardResponseDto.DashboardEquipmentStatusDto;
import com.mes.backend.dto.dashboard.DashboardResponseDto.DashboardHourlyProductionDto;
import com.mes.backend.dto.dashboard.DashboardResponseDto.DashboardKpiDto;
import com.mes.backend.dto.dashboard.DashboardResponseDto.DashboardMaterialStatusDto;
import com.mes.backend.dto.dashboard.DashboardResponseDto.DashboardWorkerStatusDto;
import com.mes.backend.dto.dashboard.DashboardResponseDto.DashboardWorkerSummaryDto;
import com.mes.backend.dto.dashboard.DashboardResponseDto.DashboardYieldDto;
import com.mes.backend.entity.DefectType;
import com.mes.backend.entity.Employee;
import com.mes.backend.entity.Equipment;
import com.mes.backend.entity.Material;
import com.mes.backend.entity.ProductLot;
import com.mes.backend.entity.QualityInspection;
import com.mes.backend.repository.EmployeeRepository;
import com.mes.backend.repository.EquipmentRepository;
import com.mes.backend.repository.DefectTypeRepository;
import com.mes.backend.repository.MaterialLotRepository;
import com.mes.backend.repository.MaterialRepository;
import com.mes.backend.repository.ProductLotRepository;
import com.mes.backend.repository.QualityInspectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH:00");
    private static final List<String> DEFECT_COLORS = List.of(
            "#2563eb",
            "#d97706",
            "#059669",
            "#dc2626",
            "#7c3aed",
            "#0891b2",
            "#db2777",
            "#64748b"
    );
    private static final List<String> DASHBOARD_EXCLUDED_DEFECT_NAMES = List.of("정렬불량", "체결불량");
    private static final int TOTAL_PROCESS_COUNT = 6;
    private static final int PROCESS_TYPE_PACKAGING = 54;

    private final ProductLotRepository productLotRepository;
    private final QualityInspectionRepository qualityInspectionRepository;
    private final EquipmentRepository equipmentRepository;
    private final DefectTypeRepository defectTypeRepository;
    private final MaterialRepository materialRepository;
    private final MaterialLotRepository materialLotRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public DashboardResponseDto getDashboard() {
        LocalDate today = LocalDate.now();
        List<ProductLot> lots = productLotRepository.findAllForReportPage();
        List<QualityInspection> inspections = qualityInspectionRepository.findAllForQualityPage();
        List<QualityInspection> todayInspections = inspections.stream()
                .filter(inspection -> isToday(inspection.getInspectionAt(), today))
                .toList();
        List<Equipment> equipment = equipmentRepository.search(null, null);
        List<Employee> employees = employeeRepository.findAll();

        int todayProductionQty = lots.stream()
                .filter(lot -> isToday(lot.getLotCreatedAt(), today))
                .mapToInt(lot -> lot.getCurrentQty() != null ? lot.getCurrentQty() : 0)
                .sum();
        /* goodQty/defectQty는 유닛(productLot+unitSequence) 단위 최종 판정 기준: 유닛 하나가
         * 거친 6개 공정 판정이 전부 모였을 때, 하나라도 NG면 그 유닛은 최종 NG. 오늘 하루 전체는
         * 여러 LOT이 섞여 있으므로 unitSequence만으로 그룹핑하면 서로 다른 LOT의 같은 순번끼리
         * 뒤섞이니, (productLot, unitSequence) 복합키로 그룹핑해야 한다. */
        Collection<List<QualityInspection>> completedUnits = completedUnitGroups(todayInspections);
        int defectQty = (int) completedUnits.stream().filter(this::isUnitNg).count();
        int goodQty = completedUnits.size() - defectQty;
        int runningEquipmentCount = (int) equipment.stream().filter(this::isRunning).count();
        int totalEquipmentCount = equipment.size();
        double defectRate = rate(defectQty, goodQty + defectQty);
        double yieldRate = rate(goodQty, goodQty + defectQty);

        return DashboardResponseDto.builder()
                .kpi(DashboardKpiDto.builder()
                        .todayProductionQty(todayProductionQty)
                        .equipmentRunRate(rate(runningEquipmentCount, totalEquipmentCount))
                        .defectRate(defectRate)
                        .runningEquipmentCount(runningEquipmentCount)
                        .totalEquipmentCount(totalEquipmentCount)
                        .build())
                .yield(DashboardYieldDto.builder()
                        .yieldRate(yieldRate)
                        .goodQty(goodQty)
                        .defectQty(defectQty)
                        .build())
                .hourlyProduction(toHourlyProduction(inspections, today))
                .defectTypes(toDefectTypes(todayInspections))
                .equipmentStatus(toEquipmentStatus(equipment))
                .materialStatus(toMaterialStatus())
                .workerSummary(toWorkerSummary(employees))
                .workerStatus(toWorkerStatus(employees))
                .build();
    }

    private List<DashboardHourlyProductionDto> toHourlyProduction(List<QualityInspection> inspections, LocalDate today) {
        Map<String, long[]> quantitiesByHour = new LinkedHashMap<>();

        for (int hour = 8; hour <= 17; hour += 1) {
            quantitiesByHour.put(String.format("%02d:00", hour), new long[] { 0L, 0L });
        }

        Map<UnitKey, List<QualityInspection>> inspectionsByUnit = inspections.stream()
                .filter(inspection -> inspection.getProductLot() != null)
                .filter(inspection -> inspection.getProductLot().getId() != null)
                .filter(inspection -> inspection.getUnitSequence() != null)
                .collect(Collectors.groupingBy(
                        inspection -> new UnitKey(inspection.getProductLot().getId(), inspection.getUnitSequence())));

        for (List<QualityInspection> unitInspections : inspectionsByUnit.values()) {
            QualityInspection packagingInspection = unitInspections.stream()
                    .filter(this::isPackagingInspection)
                    .filter(inspection -> isToday(inspection.getInspectionAt(), today))
                    .findFirst()
                    .orElse(null);
            if (packagingInspection == null || packagingInspection.getInspectionAt() == null) {
                continue;
            }

            String hour = packagingInspection.getInspectionAt().format(HOUR_FORMATTER);
            long[] quantities = quantitiesByHour.computeIfAbsent(hour, ignored -> new long[] { 0L, 0L });
            if (isUnitNg(unitInspections)) {
                quantities[1] += 1;
            } else {
                quantities[0] += 1;
            }
        }

        return quantitiesByHour.entrySet().stream()
                .map(entry -> DashboardHourlyProductionDto.builder()
                        .time(entry.getKey())
                        .good(entry.getValue()[0])
                        .defect(entry.getValue()[1])
                        .build())
                .toList();
    }

    private boolean isPackagingInspection(QualityInspection inspection) {
        return inspection.getProcess() != null
                && inspection.getProcess().getProcessType() != null
                && inspection.getProcess().getProcessType() == PROCESS_TYPE_PACKAGING;
    }

    private List<DashboardDefectTypeDto> toDefectTypes(List<QualityInspection> inspections) {
        Map<Long, Long> quantitiesByTypeId = inspections.stream()
                .filter(this::isNg)
                .filter(inspection -> inspection.getDefectType() != null)
                .collect(Collectors.groupingBy(
                        inspection -> inspection.getDefectType().getId(),
                        Collectors.counting()));

        List<DefectType> defectTypes = defectTypeRepository.findAll().stream()
                .filter(defectType -> Boolean.TRUE.equals(defectType.getActive()))
                .filter(defectType -> !DASHBOARD_EXCLUDED_DEFECT_NAMES.contains(defectTypeName(defectType)))
                .sorted(Comparator.comparing(DefectType::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        return java.util.stream.IntStream.range(0, defectTypes.size())
                .mapToObj(index -> {
                    DefectType defectType = defectTypes.get(index);
                    return DashboardDefectTypeDto.builder()
                            .name(defectTypeName(defectType))
                            .value(quantitiesByTypeId.getOrDefault(defectType.getId(), 0L))
                            .color(DEFECT_COLORS.get(index % DEFECT_COLORS.size()))
                            .build();
                })
                .toList();
    }

    private List<DashboardEquipmentStatusDto> toEquipmentStatus(List<Equipment> equipment) {
        return equipment.stream()
                .sorted(Comparator.comparing(Equipment::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(item -> DashboardEquipmentStatusDto.builder()
                        .id(item.getId())
                        .equipmentName(valueOrEmpty(item.getEquipmentName()))
                        .equipmentCode(valueOrEmpty(item.getEquipmentCode()))
                        .temp(item.getCurrentTemp())
                        .humidity(item.getCurrentHumidity())
                        .volt(item.getCurrentSupplyVoltage())
                        .running(isRunning(item))
                        .build())
                .toList();
    }

    private List<DashboardMaterialStatusDto> toMaterialStatus() {
        return materialRepository.findAllForInventoryPage().stream()
                .map(material -> DashboardMaterialStatusDto.builder()
                        .id(material.getId())
                        .code(valueOrEmpty(material.getMaterialCode()))
                        .name(valueOrEmpty(material.getMaterialName()))
                        .stock(materialLotRepository.sumCurrentQuantityByMaterialId(material.getId()))
                        .unit(valueOrEmpty(material.getUnit()))
                        .build())
                .sorted(Comparator.comparing(DashboardMaterialStatusDto::getStock, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private DashboardWorkerSummaryDto toWorkerSummary(List<Employee> employees) {
        int working = (int) employees.stream().filter(this::isPresent).count();
        return DashboardWorkerSummaryDto.builder()
                .total(employees.size())
                .working(working)
                .build();
    }

    private List<DashboardWorkerStatusDto> toWorkerStatus(List<Employee> employees) {
        return employees.stream()
                .sorted(Comparator.comparing(Employee::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(employee -> DashboardWorkerStatusDto.builder()
                        .id(employee.getId())
                        .workerName(valueOrEmpty(employee.getEmployeeName()))
                        .present(isPresent(employee))
                        .build())
                .toList();
    }

    private boolean isToday(LocalDateTime dateTime, LocalDate today) {
        return dateTime != null && dateTime.toLocalDate().equals(today);
    }

    private boolean isRunning(Equipment equipment) {
        return Boolean.TRUE.equals(equipment.getActive());
    }

    private boolean isPresent(Employee employee) {
        return Boolean.TRUE.equals(employee.getActive());
    }

    private boolean isOk(QualityInspection inspection) {
        return "OK".equalsIgnoreCase(valueOrEmpty(inspection.getInspectionResult()));
    }

    private boolean isNg(QualityInspection inspection) {
        return "NG".equalsIgnoreCase(valueOrEmpty(inspection.getInspectionResult()));
    }

    /* (productLot, unitSequence) 복합키로 그룹핑해서, 6개 공정(TOTAL_PROCESS_COUNT) 판정이 전부
     * 모인 유닛만 "완성된 유닛"으로 취급한다. 아직 6개가 안 모인 유닛(진행 중)은 조용히 제외한다. */
    private Collection<List<QualityInspection>> completedUnitGroups(List<QualityInspection> inspections) {
        return inspections.stream()
                .filter(inspection -> inspection.getUnitSequence() != null)
                .collect(Collectors.groupingBy(
                        inspection -> new UnitKey(inspection.getProductLot().getId(), inspection.getUnitSequence())))
                .values().stream()
                .filter(group -> group.size() == TOTAL_PROCESS_COUNT)
                .toList();
    }

    private boolean isUnitNg(List<QualityInspection> unitInspections) {
        return unitInspections.stream().anyMatch(this::isNg);
    }

    private record UnitKey(Long productLotId, Integer unitSequence) {
    }

    private double rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String defectTypeName(DefectType defectType) {
        if (defectType == null) {
            return "기타";
        }
        if (defectType.getDefectName() != null && !defectType.getDefectName().isBlank()) {
            return defectType.getDefectName();
        }
        return valueOrEmpty(defectType.getDefectCode(), "기타");
    }

    private String valueOrEmpty(String value) {
        return valueOrEmpty(value, "");
    }

    private String valueOrEmpty(String value, String fallback) {
        return Objects.toString(value, fallback);
    }
}
