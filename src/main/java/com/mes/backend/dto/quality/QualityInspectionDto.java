package com.mes.backend.dto.quality;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QualityInspectionDto {
    private Long id;
    private String inspectedAt;
    private String result;
    private String defectCode;
    private String defectType;
    private int defectQty;
    private String lotNo;
    private String productName;
    private String workOrderNo;
    private String processCode;
    private String processName;
    private String machineCode;
    private String machineName;
    private String workerName;
    private BigDecimal voltage;
    private BigDecimal humidity;
    private List<QualityMeasurementDto> measurements;
}
