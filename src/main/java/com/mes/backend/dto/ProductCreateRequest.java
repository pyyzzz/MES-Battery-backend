// 제품 등록 요청 DTO
package com.mes.backend.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductCreateRequest {
    private String productCode;
    private String productName;
    private BigDecimal voltage;
    private BigDecimal capacity;
    private String unit;
}
