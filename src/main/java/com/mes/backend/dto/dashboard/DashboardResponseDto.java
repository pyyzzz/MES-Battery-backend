package com.mes.backend.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DashboardResponseDto {
    private DashboardKpiDto kpi;
    private DashboardYieldDto yield;
    private List<DashboardHourlyProductionDto> hourlyProduction;
    private List<DashboardDefectTypeDto> defectTypes;
    private List<DashboardEquipmentStatusDto> equipmentStatus;
    private List<DashboardMaterialStatusDto> materialStatus;
    private DashboardWorkerSummaryDto workerSummary;
    private List<DashboardWorkerStatusDto> workerStatus;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DashboardKpiDto {
        private int todayProductionQty;
        private double equipmentRunRate;
        private double defectRate;
        private int runningEquipmentCount;
        private int totalEquipmentCount;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DashboardYieldDto {
        private double yieldRate;
        private int goodQty;
        private int defectQty;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DashboardHourlyProductionDto {
        private String time;
        private long good;
        private long defect;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DashboardDefectTypeDto {
        private String name;
        private long value;
        private String color;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DashboardEquipmentStatusDto {
        private Long id;
        private String equipmentName;
        private String equipmentCode;
        private BigDecimal temp;
        private BigDecimal humidity;
        private BigDecimal volt;
        private boolean running;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DashboardMaterialStatusDto {
        private Long id;
        private String code;
        private String name;
        private BigDecimal stock;
        private String unit;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DashboardWorkerSummaryDto {
        private int total;
        private int working;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DashboardWorkerStatusDto {
        private Long id;
        private String workerName;
        private boolean present;
    }
}
