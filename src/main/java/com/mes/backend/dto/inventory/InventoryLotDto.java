package com.mes.backend.dto.inventory;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class InventoryLotDto {
    private Long id;
    private String inboundAt;
    private String status;
    private String lotNo;
    private String materialCode;
    private String materialName;
    private String unit;
    private BigDecimal totalStock;
    private BigDecimal consumed;
    private BigDecimal remaining;
    private double consumptionRate;
    private String updatedAt;
    private List<InventoryLotUsageDto> usages;
}
