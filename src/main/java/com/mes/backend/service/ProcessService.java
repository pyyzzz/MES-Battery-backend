// 공정(Process) 마스터 CRUD
package com.mes.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.ProcessCreateRequest;
import com.mes.backend.dto.ProcessUpdateRequest;
import com.mes.backend.entity.Employee;
import com.mes.backend.entity.Process;
import com.mes.backend.repository.EmployeeRepository;
import com.mes.backend.repository.ProcessRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcessService {

    private final ProcessRepository processRepo;
    private final EmployeeRepository employeeRepo;

    public List<Process> search(String processName, String processStatus) {
        return processRepo.search(processName, processStatus);
    }

    public Process getById(Long id) {
        return processRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("공정을 찾을 수 없습니다. ID: " + id));
    }

    @Transactional
    public Process create(ProcessCreateRequest request) {
        Employee manager = findEmployee(request.getManagerEmployeeId());
        return processRepo.save(Process.builder()
                .processCode(request.getProcessCode())
                .processName(request.getProcessName())
                .sequenceNo(request.getSequenceNo())
                .managerEmployee(manager)
                .description(request.getDescription())
                .build());
    }

    /* process_code는 잠금 - 여기서 건드리지 않음 */
    @Transactional
    public Process update(Long id, ProcessUpdateRequest request) {
        Process process = getById(id);
        Employee manager = findEmployee(request.getManagerEmployeeId());
        process.setProcessName(request.getProcessName());
        process.setSequenceNo(request.getSequenceNo());
        process.setManagerEmployee(manager);
        process.setDescription(request.getDescription());
        return processRepo.save(process);
    }

    private Employee findEmployee(Long employeeId) {
        return employeeRepo.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("담당자를 찾을 수 없습니다. ID: " + employeeId));
    }
}
