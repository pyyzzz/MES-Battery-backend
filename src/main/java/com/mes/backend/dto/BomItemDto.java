// BOM 항목 조회/저장 응답 DTO
package com.mes.backend.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BomItemDto {
    private Long id;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String unit;
    private BigDecimal requiredQuantity;
    private Long inputProcessId;
    private String inputProcessName;
}
