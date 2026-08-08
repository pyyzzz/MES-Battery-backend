// 작업자 등록 요청 DTO (employee_no/username/password는 서버가 자동 생성)
package com.mes.backend.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WorkerCreateRequest {
    private String employeeName;
    private LocalDate hireDate;
    private String role;
}
