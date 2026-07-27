// 설비(Equipment) 마스터 CRUD
package com.mes.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.EquipmentCreateRequest;
import com.mes.backend.dto.EquipmentUpdateRequest;
import com.mes.backend.dto.ProductionReportDto;
import com.mes.backend.entity.Equipment;
import com.mes.backend.entity.Process;
import com.mes.backend.exception.CustomException;
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

    @PreAuthorize("hasAuthority('관리자')")
    @Transactional
    public Equipment create(EquipmentCreateRequest request) {
        Process process = findProcess(request.getProcessId());
        ensureProcessAvailable(process, null);
        return equipmentRepo.save(Equipment.builder()
                .process(process)
                .equipmentCode(request.getEquipmentCode())
                .equipmentName(request.getEquipmentName())
                .equipmentStatus(request.getEquipmentStatus())
                .active(request.getActive())
                .statusMessage(request.getStatusMessage())
                .build());
    }

    /* equipment_code는 잠금 - 여기서 건드리지 않음 */
    @PreAuthorize("hasAuthority('관리자')")
    @Transactional
    public Equipment update(Long id, EquipmentUpdateRequest request) {
        Equipment equipment = getById(id);
        Process process = findProcess(request.getProcessId());
        ensureProcessAvailable(process, id);
        equipment.setProcess(process);
        equipment.setEquipmentName(request.getEquipmentName());
        equipment.setEquipmentStatus(request.getEquipmentStatus());
        equipment.setActive(request.getActive());
        equipment.setStatusMessage(request.getStatusMessage());
        return equipmentRepo.save(equipment);
    }

    /* 환경센서(온도0x10/습도0x11/설비전원전압0x12) 리포트 - 최신값 덮어쓰기, QualityInspection/BOM차감/판정 로직과 무관 */
    private static final int PROCESS_TYPE_TEMP = 16;
    private static final int PROCESS_TYPE_HUMIDITY = 17;
    private static final int PROCESS_TYPE_SUPPLY_VOLTAGE = 18;

    @Transactional
    public void reportEnvironment(ProductionReportDto dto) {
        Equipment equipment = equipmentRepo.findByEquipmentPort(parsePort(dto.getMachineId()))
                .orElseThrow(() -> new RuntimeException("포트에 해당하는 설비를 찾을 수 없습니다. machineId=" + dto.getMachineId()));

        BigDecimal value = BigDecimal.valueOf(dto.getValue());
        switch (dto.getProcessType()) {
            case PROCESS_TYPE_TEMP -> equipment.setCurrentTemp(value);
            case PROCESS_TYPE_HUMIDITY -> equipment.setCurrentHumidity(value);
            case PROCESS_TYPE_SUPPLY_VOLTAGE -> equipment.setCurrentSupplyVoltage(value);
            default -> throw new IllegalArgumentException("환경센서 리포트가 아닌 processType입니다: " + dto.getProcessType());
        }
        equipment.setLastReportedAt(LocalDateTime.now());
        equipmentRepo.save(equipment);
    }

    private int parsePort(String machineId) {
        String portPart = machineId.substring(machineId.lastIndexOf('-') + 1);
        return Integer.parseInt(portPart);
    }

    private Process findProcess(Long processId) {
        return processRepo.findById(processId)
                .orElseThrow(() -> new RuntimeException("공정을 찾을 수 없습니다. ID: " + processId));
    }

    /* 1:1 관계라 이미 다른 설비가 배정된 공정은 선택 불가 (수정 시 자기 자신은 제외) */
    private void ensureProcessAvailable(Process process, Long currentEquipmentId) {
        Optional<Equipment> occupied = equipmentRepo.findByProcess_Id(process.getId());
        if (occupied.isPresent() && !occupied.get().getId().equals(currentEquipmentId)) {
            throw new CustomException("PROCESS_OCCUPIED", "이미 다른 설비가 배정된 공정입니다: " + process.getProcessCode());
        }
    }
}
