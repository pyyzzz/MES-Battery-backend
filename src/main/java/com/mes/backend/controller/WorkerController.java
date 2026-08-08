// 작업자(Employee) 마스터 CRUD + 본인 비밀번호 변경 API
package com.mes.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mes.backend.dto.PasswordChangeRequest;
import com.mes.backend.dto.WorkerCreateRequest;
import com.mes.backend.dto.WorkerUpdateRequest;
import com.mes.backend.entity.Employee;
import com.mes.backend.service.WorkerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mes/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;

    @PostMapping
    public Employee create(@RequestBody WorkerCreateRequest request) {
        return workerService.create(request);
    }

    @PreAuthorize("hasAuthority('관리자')")
    @GetMapping
    public List<Employee> search(@RequestParam(required = false) LocalDate hireDateFrom,
                                  @RequestParam(required = false) LocalDate hireDateTo,
                                  @RequestParam(required = false) String role) {
        return workerService.search(hireDateFrom, hireDateTo, role);
    }

    @GetMapping("/{id}")
    public Employee getById(@PathVariable Long id) {
        return workerService.getById(id);
    }

    @PutMapping("/{id}")
    public Employee update(@PathVariable Long id, @RequestBody WorkerUpdateRequest request) {
        return workerService.update(id, request);
    }

    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(Authentication authentication,
                                                @RequestBody PasswordChangeRequest request) {
        workerService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok().build();
    }
}
