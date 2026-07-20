package com.mes.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MaterialInboundDto {
    private String code;
    private String name;
    private int amount;
}
