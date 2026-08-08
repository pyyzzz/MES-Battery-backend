package com.mes.backend.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ReportSummaryDto {
    private int lotCount;
    private int planQty;
    private int actualQty;
    private int goodQty;
    private int defectQty;
    private double achievementRate;
    private double yieldRate;
}
