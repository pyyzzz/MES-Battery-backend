package com.mes.backend.dto.quality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QualityTrendDto {
    private String date;
    private long ok;
    private long ng;
}
