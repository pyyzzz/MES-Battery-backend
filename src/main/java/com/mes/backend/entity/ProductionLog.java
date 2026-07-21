package com.mes.backend.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionLog {
    private Long id;

    private WorkOrder workOrder;

    private String productCode;
    private String machineId;

    private String serialNo;

    private String result;
    private String defectCode;

    private LocalDateTime productAt;
}
