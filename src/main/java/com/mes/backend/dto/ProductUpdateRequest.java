// 제품 수정 요청 DTO (product_code는 잠금이라 여기 없음)
package com.mes.backend.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductUpdateRequest {
    private String productName;
    private BigDecimal voltage;
    private BigDecimal capacity;
    private String unit;
}
