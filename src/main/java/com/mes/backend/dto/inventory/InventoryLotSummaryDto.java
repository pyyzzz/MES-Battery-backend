package com.mes.backend.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class InventoryLotSummaryDto {
    private long total;
    private long inUse;
    private long waiting;
    private long defect;
}
