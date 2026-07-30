package com.mes.backend.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ReportDailyProductionDto {
    private String date;
    private int plan;
    private int actual;
    private int good;
}
