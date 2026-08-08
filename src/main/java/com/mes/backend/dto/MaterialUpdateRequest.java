// 자재 수정 요청 DTO (material_code는 잠금이라 여기 없음)
package com.mes.backend.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MaterialUpdateRequest {
    private String materialName;
    private String unit;
    private BigDecimal safetyStock;
}
