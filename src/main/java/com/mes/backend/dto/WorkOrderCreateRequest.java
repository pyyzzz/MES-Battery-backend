package com.mes.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderCreateRequest {
    private String productCode;
    private int targetQty;
}
