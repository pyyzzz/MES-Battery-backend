package com.mes.backend.dto.inventory;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class InventoryTransactionSummaryDto {
    private BigDecimal inbound;
    private BigDecimal consumption;
    private double ratio;
}
