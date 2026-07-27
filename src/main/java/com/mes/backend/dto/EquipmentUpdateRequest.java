// 설비 수정 요청 DTO (equipment_code는 잠금이라 여기 없음, 실시간 값 필드도 없음)
package com.mes.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EquipmentUpdateRequest {
    private String equipmentName;
    private Long processId;
    private String equipmentStatus;
    private Boolean active;
    private String statusMessage;
}
