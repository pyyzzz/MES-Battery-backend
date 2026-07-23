package com.mes.backend.dto.report;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ReportMaterialDto {
    private String materialCode;
    private String materialName;
    private String materialLotNo;
    private BigDecimal quantity;
    private String unit;
}
