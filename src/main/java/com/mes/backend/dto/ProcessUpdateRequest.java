// 공정 수정 요청 DTO (process_code는 잠금이라 여기 없음)
package com.mes.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProcessUpdateRequest {
    private String processName;
    private Integer sequenceNo;
    private Long managerEmployeeId;
    private String equipmentCode;
    private String description;
    private String processStatus;
}
