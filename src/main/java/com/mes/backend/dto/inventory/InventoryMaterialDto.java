package com.mes.backend.dto.inventory;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class InventoryMaterialDto {
    private Long id;
    private String code;
    private String name;
    private BigDecimal stock;
    private BigDecimal safetyStock;
    private String unit;
    private String status;
    private String registeredAt;
    private String lastInboundAt;
    private String location;
    private String lotNo;
    private List<InventoryMaterialLotSummaryDto> lots;
}
