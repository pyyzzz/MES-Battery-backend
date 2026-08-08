// 작업지시 등록(화면용) 요청 DTO - manager_employee_id는 로그인 세션에서 자동 채움
package com.mes.backend.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WorkOrderRegisterRequest {
    private Long bomId;
    private Integer orderQuantity;
    private LocalDate dueDate;
}
