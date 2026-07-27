// 공정 등록 요청 DTO
package com.mes.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProcessCreateRequest {
    private String processCode;
    private String processName;
    private Integer sequenceNo;
    private Long managerEmployeeId;
    private String description;
    private String processStatus;
}
