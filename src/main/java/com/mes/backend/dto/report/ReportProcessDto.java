package com.mes.backend.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ReportProcessDto {
    private String processCode;
    private String processName;
    private String equipmentName;
    private String workerName;
    private String startedAt;
    private String endedAt;
    private String result;
}
