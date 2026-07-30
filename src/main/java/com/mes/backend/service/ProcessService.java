// 공정(Process) 마스터 CRUD
package com.mes.backend.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.ProcessCreateRequest;
import com.mes.backend.dto.ProcessUpdateRequest;
import com.mes.backend.entity.Employee;
import com.mes.backend.entity.Equipment;
import com.mes.backend.entity.Process;
import com.mes.backend.repository.EmployeeRepository;
import com.mes.backend.repository.EquipmentRepository;
import com.mes.backend.repository.ProcessRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcessService {

    private final ProcessRepository processRepo;
    private final EmployeeRepository employeeRepo;
    private final EquipmentRepository equipmentRepo;

    public List<Process> search(String processName, String processStatus) {
        return processRepo.search(processName, processStatus);
    }

    public Process getById(Long id) {
        return processRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("공정을 찾을 수 없습니다. ID: " + id));
    }

    @PreAuthorize("hasAuthority('관리자')")
    @Transactional
    public Process create(ProcessCreateRequest request) {
        Employee manager = findEmployee(request.getManagerEmployeeId());
        Process process = processRepo.save(Process.builder()
                .processCode(request.getProcessCode())
                .processName(request.getProcessName())
                .sequenceNo(request.getSequenceNo())
                .managerEmployee(manager)
                .description(request.getDescription())
                .processStatus(request.getProcessStatus() != null ? request.getProcessStatus() : "사용")
                .build());
        assignEquipment(process, request.getEquipmentCode());
        return process;
    }

    /* process_code는 잠금 - 여기서 건드리지 않음.
     * processStatus는 프론트가 아직 안 보내는 경우가 있어(null) 그때는 기존 값 유지 */
    @PreAuthorize("hasAuthority('관리자')")
    @Transactional
    public Process update(Long id, ProcessUpdateRequest request) {
        Process process = getById(id);
        Employee manager = findEmployee(request.getManagerEmployeeId());
        process.setProcessName(request.getProcessName());
        process.setSequenceNo(request.getSequenceNo());
        process.setManagerEmployee(manager);
        process.setDescription(request.getDescription());
        if (request.getProcessStatus() != null) {
            process.setProcessStatus(request.getProcessStatus());
        }
        Process saved = processRepo.save(process);
        assignEquipment(saved, request.getEquipmentCode());
        return saved;
    }

    @PreAuthorize("hasAuthority('관리자')")
    @Transactional
    public void delete(Long id) {
        Process process = getById(id);
        process.setProcessStatus("INACTIVE");
        processRepo.save(process);
    }

    private Employee findEmployee(Long employeeId) {
        return employeeRepo.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("담당자를 찾을 수 없습니다. ID: " + employeeId));
    }

    private void assignEquipment(Process process, String equipmentCode) {
        if (equipmentCode == null || equipmentCode.isBlank() || "설비 선택 (없음)".equals(equipmentCode)) {
            return;
        }

        Equipment equipment = equipmentRepo.findByEquipmentCode(equipmentCode)
                .orElseThrow(() -> new RuntimeException("설비를 찾을 수 없습니다. code: " + equipmentCode));
        Process previousProcess = equipment.getProcess();
        equipmentRepo.findByProcess_Id(process.getId())
                .filter(currentEquipment -> !currentEquipment.getId().equals(equipment.getId()))
                .ifPresent(currentEquipment -> {
                    if (previousProcess != null) {
                        currentEquipment.setProcess(previousProcess);
                        equipmentRepo.save(currentEquipment);
                    }
                });
        equipment.setProcess(process);
        equipmentRepo.save(equipment);
    }
}
