package com.mes.backend.dto.quality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QualitySummaryDto {
    private long total;
    private long ok;
    private long ng;
    private int okRate;
    private String topDefect;
}
