// 완제품LOT 상세 조회 응답 DTO (목록 정보 + 검사 요약 + 공정/자재 이력)
package com.mes.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.mes.backend.dto.report.ReportMaterialDto;
import com.mes.backend.dto.report.ReportProcessDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProductLotDetailDto {
    private Long id;
    private String productLotNo;
    private String productName;
    private String workOrderNo;
    private Integer currentQty;
    private String lotStatus;
    private LocalDateTime lotCreatedAt;
    private long inspectionCount;
    private long passCount;
    private long failCount;
    private List<ReportProcessDto> processes;
    private List<ReportMaterialDto> materials;
}
