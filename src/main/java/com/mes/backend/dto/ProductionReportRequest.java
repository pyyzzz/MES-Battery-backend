package com.mes.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductionReportRequest {
    private Long orderId;
    private String machineId;
    private String result;
    private String defectCode;
    private String serialNo;
}
