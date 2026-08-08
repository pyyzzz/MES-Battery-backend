package com.mes.backend.dto.report;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ReportLotDto {
    private Long id;
    private String productionDate;
    private String lotNo;
    private String productCode;
    private String productName;
    private String workOrderNo;
    private int planQty;
    private int actualQty;
    private int goodQty;
    private int defectQty;
    private String status;
    private String equipment;
    private double yieldRate;
    private List<ReportProcessDto> processes;
    private List<ReportMaterialDto> materials;
    private ReportQualityDto quality;
}
