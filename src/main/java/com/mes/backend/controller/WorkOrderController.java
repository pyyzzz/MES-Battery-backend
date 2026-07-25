// 작업지시(WorkOrder) 화면 조회/등록 API - 레거시 /api/mes/order(s)와는 별개 경로
package com.mes.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mes.backend.dto.WorkOrderRegisterRequest;
import com.mes.backend.entity.WorkOrder;
import com.mes.backend.service.WorkOrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mes/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @GetMapping
    public List<WorkOrder> search(@RequestParam(required = false) String workOrderNo,
                                   @RequestParam(required = false) String workOrderStatus,
                                   @RequestParam(required = false) LocalDate dueDate) {
        return workOrderService.search(workOrderNo, workOrderStatus, dueDate);
    }

    @GetMapping("/{id}")
    public WorkOrder getById(@PathVariable Long id) {
        return workOrderService.getById(id);
    }

    @PostMapping
    public WorkOrder create(Authentication authentication, @RequestBody WorkOrderRegisterRequest request) {
        return workOrderService.create(request, authentication.getName());
    }
}
