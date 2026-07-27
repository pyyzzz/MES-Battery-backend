// 완제품LOT 목록 조회 응답 DTO
package com.mes.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProductLotSummaryDto {
    private Long id;
    private String productLotNo;
    private String productName;
    private String workOrderNo;
    private Integer currentQty;
    private String lotStatus;
    private LocalDateTime lotCreatedAt;
}
