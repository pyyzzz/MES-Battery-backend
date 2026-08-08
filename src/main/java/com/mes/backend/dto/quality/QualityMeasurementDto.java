package com.mes.backend.dto.quality;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QualityMeasurementDto {
    private String measurementCode;
    private BigDecimal measuredValue;
    private String unit;
}
