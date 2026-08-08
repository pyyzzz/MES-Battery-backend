package com.mes.backend.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ReportProcessChartDto {
    private String process;
    private long output;
    private long defect;
}
