// 설비 등록 요청 DTO (실시간 값 필드는 reportProduction() 전용이라 여기 없음)
package com.mes.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EquipmentCreateRequest {
    private String equipmentCode;
    private String equipmentName;
    private Long processId;
    private String equipmentStatus;
    private Boolean active;
    private String statusMessage;
}
