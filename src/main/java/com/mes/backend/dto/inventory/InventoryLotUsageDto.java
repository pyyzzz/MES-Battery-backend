package com.mes.backend.dto.inventory;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class InventoryLotUsageDto {
    private String occurredAt;
    private String productLotNo;
    private BigDecimal quantity;
}
