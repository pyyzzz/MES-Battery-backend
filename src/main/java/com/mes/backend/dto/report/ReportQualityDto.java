package com.mes.backend.dto.report;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ReportQualityDto {
    private String result;
    private String inspectedAt;
    private String inspectorName;
    private String defectCode;
    private String defectType;
    private int defectQty;
    private BigDecimal voltage;
    private BigDecimal resistance;
}
