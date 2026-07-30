// 작업자(Employee) 마스터 CRUD + 본인 비밀번호 변경
package com.mes.backend.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.PasswordChangeRequest;
import com.mes.backend.dto.WorkerCreateRequest;
import com.mes.backend.dto.WorkerUpdateRequest;
import com.mes.backend.entity.Employee;
import com.mes.backend.exception.CustomException;
import com.mes.backend.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkerService {

    private static final String DEFAULT_PASSWORD = "1234";

    private final EmployeeRepository employeeRepo;
    private final PasswordEncoder passwordEncoder;

    public List<Employee> search(LocalDate hireDateFrom, LocalDate hireDateTo, String role) {
        return employeeRepo.search(hireDateFrom, hireDateTo, role);
    }

    public Employee getById(Long id) {
        return employeeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("작업자를 찾을 수 없습니다. ID: " + id));
    }

    @PreAuthorize("hasAuthority('관리자')")
    @Transactional
    public Employee create(WorkerCreateRequest request) {
        String employeeNo = generateEmployeeNo(request.getHireDate());
        return employeeRepo.save(Employee.builder()
                .employeeNo(employeeNo)
                .employeeName(request.getEmployeeName())
                .role(request.getRole())
                .hireDate(request.getHireDate())
                .active(true)
                .username(employeeNo)
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .build());
    }

    /* employee_no/username/password는 여기서 안 건드림 (잠금/별도 API) */
    @PreAuthorize("hasAuthority('관리자')")
    @Transactional
    public Employee update(Long id, WorkerUpdateRequest request) {
        Employee employee = getById(id);
        employee.setEmployeeName(request.getEmployeeName());
        employee.setHireDate(request.getHireDate());
        employee.setRole(request.getRole());
        employee.setActive(request.getActive());
        return employeeRepo.save(employee);
    }

    @PreAuthorize("hasAuthority('관리자')")
    @Transactional
    public void delete(Long id) {
        Employee employee = getById(id);
        employee.setActive(false);
        employeeRepo.save(employee);
    }

    @Transactional
    public void changePassword(String username, PasswordChangeRequest request) {
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new CustomException("PASSWORD_MISMATCH", "새 비밀번호와 확인이 일치하지 않습니다.");
        }
        Employee employee = employeeRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("계정을 찾을 수 없습니다: " + username));
        if (!passwordEncoder.matches(request.getCurrentPassword(), employee.getPassword())) {
            throw new CustomException("INVALID_PASSWORD", "현재 비밀번호가 일치하지 않습니다.");
        }
        employee.setPassword(passwordEncoder.encode(request.getNewPassword()));
        employeeRepo.save(employee);
    }

    /* "W-" + hireDate(yyMMdd) + "-" + 순번(4자리), 순번은 같은 hireDate 개수+1 */
    private String generateEmployeeNo(LocalDate hireDate) {
        String datePart = DateTimeFormatter.ofPattern("yyMMdd").format(hireDate);
        long sequence = employeeRepo.countByHireDate(hireDate) + 1;
        return "W-" + datePart + "-" + String.format("%04d", sequence);
    }
}
