package com.mes.backend.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class InventoryMaterialSummaryDto {
    private long total;
    private long safe;
    private long warning;
    private long danger;
}
