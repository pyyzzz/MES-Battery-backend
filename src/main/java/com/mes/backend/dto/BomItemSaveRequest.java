// BOM 항목 일괄 저장 요청 DTO (id 없으면 신규, 있으면 수정)
package com.mes.backend.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BomItemSaveRequest {
    private Long id;
    private Long materialId;
    private Long inputProcessId;
    private BigDecimal requiredQuantity;
}
