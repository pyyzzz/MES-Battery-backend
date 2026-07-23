// 설비(Equipment) 마스터 CRUD
package com.mes.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.EquipmentCreateRequest;
import com.mes.backend.dto.EquipmentUpdateRequest;
import com.mes.backend.entity.Equipment;
import com.mes.backend.entity.Process;
import com.mes.backend.repository.EquipmentRepository;
import com.mes.backend.repository.ProcessRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepo;
    private final ProcessRepository processRepo;

    public List<Equipment> search(String equipmentName, String equipmentStatus) {
        return equipmentRepo.search(equipmentName, equipmentStatus);
    }

    public Equipment getById(Long id) {
        return equipmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("설비를 찾을 수 없습니다. ID: " + id));
    }

    @Transactional
    public Equipment create(EquipmentCreateRequest request) {
        Process process = findProcess(request.getProcessId());
        ensureProcessAvailable(process, null);
        return equipmentRepo.save(Equipment.builder()
                .process(process)
                .equipmentCode(request.getEquipmentCode())
                .equipmentName(request.getEquipmentName())
                .active(request.getActive())
                .statusMessage(request.getStatusMessage())
                .build());
    }

    /* equipment_code는 잠금 - 여기서 건드리지 않음 */
    @Transactional
    public Equipment update(Long id, EquipmentUpdateRequest request) {
        Equipment equipment = getById(id);
        Process process = findProcess(request.getProcessId());
        ensureProcessAvailable(process, id);
        equipment.setProcess(process);
        equipment.setEquipmentName(request.getEquipmentName());
        equipment.setActive(request.getActive());
        equipment.setStatusMessage(request.getStatusMessage());
        return equipmentRepo.save(equipment);
    }

    private Process findProcess(Long processId) {
        return processRepo.findById(processId)
                .orElseThrow(() -> new RuntimeException("공정을 찾을 수 없습니다. ID: " + processId));
    }

    /* 1:1 관계라 이미 다른 설비가 배정된 공정은 선택 불가 (수정 시 자기 자신은 제외) */
    private void ensureProcessAvailable(Process process, Long currentEquipmentId) {
        Optional<Equipment> occupied = equipmentRepo.findByProcess_Id(process.getId());
        if (occupied.isPresent() && !occupied.get().getId().equals(currentEquipmentId)) {
            throw new RuntimeException("이미 다른 설비가 배정된 공정입니다: " + process.getProcessCode());
        }
    }
}
