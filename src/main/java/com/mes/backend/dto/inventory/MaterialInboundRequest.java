package com.mes.backend.dto.inventory;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MaterialInboundRequest {
    private BigDecimal quantity;
    private Long employeeId;
}
