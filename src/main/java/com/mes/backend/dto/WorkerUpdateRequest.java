// 작업자 수정 요청 DTO (employee_no/username/password는 여기 없음 - 잠금/별도 API)
package com.mes.backend.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WorkerUpdateRequest {
    private String employeeName;
    private LocalDate hireDate;
    private String role;
    private Boolean active;
}
