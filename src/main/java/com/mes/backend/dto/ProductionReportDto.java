// L2 -> 백엔드 생산 리포트 수신 DTO (공정별 원시 측정값, 판정은 백엔드가 함)
package com.mes.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductionReportDto {
    private Long orderId;
    private String machineId;
    private String serialNo;
    private Integer processType;

    /* 단일값 공정(전극/조립/활성화/팩/포장) 전용, nullable */
    private Integer value;

    /* 검사공정(processType=53) 전용 3필드, nullable */
    private Integer voltageX100;
    private Integer capacityX1000;
    private Integer resistanceMohm;
}
