package com.mes.backend.dto.inventory;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class InventoryMaterialLotSummaryDto {
    private Long id;
    private String lotNo;
    private String inboundAt;
    private BigDecimal initialQuantity;
    private BigDecimal currentQuantity;
    private String status;
}
