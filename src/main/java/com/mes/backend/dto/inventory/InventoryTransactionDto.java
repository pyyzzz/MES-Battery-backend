package com.mes.backend.dto.inventory;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class InventoryTransactionDto {
    private Long id;
    private String occurredAt;
    private String type;
    private String materialCode;
    private String materialName;
    private String unit;
    private String materialLotNo;
    private String productLotNo;
    private String processCode;
    private String processName;
    private BigDecimal quantity;
    private BigDecimal beforeStock;
    private BigDecimal afterStock;
    private String worker;
    private String note;
}
