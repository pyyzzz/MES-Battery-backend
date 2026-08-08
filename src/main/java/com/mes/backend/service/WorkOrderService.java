// 작업지시(WorkOrder) 화면 조회/등록 - assignWorkToMachine()/reportProduction()의 실물 생산 흐름과는 별개
package com.mes.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.WorkOrderRegisterRequest;
import com.mes.backend.entity.Bom;
import com.mes.backend.entity.Employee;
import com.mes.backend.entity.WorkOrder;
import com.mes.backend.repository.BomRepository;
import com.mes.backend.repository.EmployeeRepository;
import com.mes.backend.repository.WorkOrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository orderRepo;
    private final BomRepository bomRepo;
    private final EmployeeRepository employeeRepo;

    public List<WorkOrder> search(String workOrderNo, String workOrderStatus, LocalDate dueDate) {
        return orderRepo.search(workOrderNo, workOrderStatus, dueDate);
    }

    public WorkOrder getById(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("작업지시를 찾을 수 없습니다. ID: " + id));
    }

    /* manager_employee_id는 폼 입력이 아니라 로그인 세션(username)으로 조회한 Employee를 그대로 사용 */
    @PreAuthorize("hasAuthority('관리자')")
    @Transactional
    public WorkOrder create(WorkOrderRegisterRequest request, String username) {
        Bom bom = bomRepo.findById(request.getBomId())
                .orElseThrow(() -> new RuntimeException("BOM을 찾을 수 없습니다. ID: " + request.getBomId()));
        Employee manager = employeeRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("계정을 찾을 수 없습니다: " + username));

        return orderRepo.save(WorkOrder.builder()
                .bom(bom)
                .managerEmployee(manager)
                .workOrderNo(generateWorkOrderNo())
                .orderQuantity(request.getOrderQuantity())
                .dueDate(request.getDueDate())
                .workOrderStatus("WAITING")
                .build());
    }

    /* "WO-" + 현재분(yyMMddHHmm) + "-" + 순번, 순번은 같은 분 안 기존 건수+1 (ProductLot 채번과 동일 방식) */
    private String generateWorkOrderNo() {
        String prefix = "WO-" + DateTimeFormatter.ofPattern("yyMMddHHmm").format(LocalDateTime.now());
        long sequence = orderRepo.countByWorkOrderNoStartingWith(prefix) + 1;
        return prefix + "-" + sequence;
    }
}
