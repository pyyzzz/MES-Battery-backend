package com.mes.backend.dto.quality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QualityDefectDistributionDto {
    private String name;
    private long value;
}
