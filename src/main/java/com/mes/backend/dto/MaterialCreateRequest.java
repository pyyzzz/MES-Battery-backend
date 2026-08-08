// 자재 등록 요청 DTO
package com.mes.backend.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MaterialCreateRequest {
    private String materialCode;
    private String materialName;
    private String unit;
    private BigDecimal safetyStock;
}
