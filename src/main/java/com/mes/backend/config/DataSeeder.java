// 로컬 테스트용 기본 계정 + 마스터 시드 (서버 기동 시 없으면 1회 생성)
package com.mes.backend.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.mes.backend.entity.DefectType;
import com.mes.backend.entity.Employee;
import com.mes.backend.entity.Equipment;
import com.mes.backend.entity.MeasurementSpec;
import com.mes.backend.entity.Process;
import com.mes.backend.repository.DefectTypeRepository;
import com.mes.backend.repository.EmployeeRepository;
import com.mes.backend.repository.EquipmentRepository;
import com.mes.backend.repository.MeasurementSpecRepository;
import com.mes.backend.repository.ProcessRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProcessRepository processRepository;
    private final EquipmentRepository equipmentRepository;
    private final DefectTypeRepository defectTypeRepository;
    private final MeasurementSpecRepository measurementSpecRepository;

    @Override
    public void run(String... args) {
        Employee admin = seedAdmin();
        seedProcesses(admin);
        seedEquipment();
        seedDefectTypes();
        seedMeasurementSpecs();
    }

    /* 이미 존재하는 admin row에 role 등 필드가 누락돼 있으면 재기동 시 자동으로 채워준다(자가치유) */
    private Employee seedAdmin() {
        Employee admin = employeeRepository.findByUsername("admin")
                .orElseGet(() -> Employee.builder()
                        .employeeNo("ADMIN")
                        .employeeName("관리자")
                        .username("admin")
                        .password(passwordEncoder.encode("1234"))
                        .build());
        if (admin.getRole() == null) {
            admin.setRole("관리자");
        }
        if (admin.getEmployeeName() == null || admin.getEmployeeName().isBlank()) {
            admin.setEmployeeName("관리자");
        }
        if (admin.getEmployeeNo() == null || admin.getEmployeeNo().isBlank()) {
            admin.setEmployeeNo("ADMIN");
        }
        return employeeRepository.save(admin);
    }

    /* 담당자 마스터(Worker)가 아직 없어 PROCESS.managerEmployee는 임시로 admin 계정을 지정 */
    private void seedProcesses(Employee manager) {
        seedProcess(49, "PROC-010", "전극공정", 1, manager);
        seedProcess(50, "PROC-020", "조립공정", 2, manager);
        seedProcess(51, "PROC-030", "활성화공정", 3, manager);
        seedProcess(52, "PROC-040", "팩공정", 4, manager);
        seedProcess(53, "PROC-050", "검사공정", 5, manager);
        seedProcess(54, "PROC-060", "포장공정", 6, manager);
    }

    private void seedProcess(int processType, String processCode, String processName, int sequenceNo, Employee manager) {
        if (processRepository.findByProcessType(processType).isPresent()) {
            return;
        }
        processRepository.save(Process.builder()
                .processType(processType)
                .processCode(processCode)
                .processName(processName)
                .sequenceNo(sequenceNo)
                .managerEmployee(manager)
                .build());
    }

    private void seedEquipment() {
        seedEquipmentFor(49, "EQ-010", "전극설비", 5006);
        seedEquipmentFor(50, "EQ-020", "조립설비", 5007);
        seedEquipmentFor(51, "EQ-030", "활성화설비", 5008);
        seedEquipmentFor(52, "EQ-040", "팩설비", 5009);
        seedEquipmentFor(53, "EQ-050", "검사설비", 5010);
        seedEquipmentFor(54, "EQ-060", "포장설비", 5011);
    }

    /* 이미 존재하는 row에 equipment_port가 누락돼 있으면 재기동 시 자동으로 채워준다(자가치유) */
    private void seedEquipmentFor(int processType, String equipmentCode, String equipmentName, int equipmentPort) {
        Equipment existing = equipmentRepository.findByEquipmentCode(equipmentCode).orElse(null);
        if (existing != null) {
            if (existing.getEquipmentPort() == null) {
                existing.setEquipmentPort(equipmentPort);
                equipmentRepository.save(existing);
            }
            return;
        }
        Process process = processRepository.findByProcessType(processType)
                .orElseThrow(() -> new IllegalStateException("Process 시드 누락: processType=" + processType));
        equipmentRepository.save(Equipment.builder()
                .process(process)
                .equipmentCode(equipmentCode)
                .equipmentName(equipmentName)
                .equipmentPort(equipmentPort)
                .build());
    }

    private void seedDefectTypes() {
        seedDefectType("THICKNESS", "두께불량");
        seedDefectType("ALIGNMENT", "정렬불량");
        seedDefectType("CURRENT", "충전불량");
        seedDefectType("TORQUE", "체결불량");
        seedDefectType("VOLTAGE", "전압불량");
        seedDefectType("CAPACITY", "용량불량");
        seedDefectType("RESISTANCE", "저항불량");
        seedDefectType("WEIGHT", "중량불량");
    }

    private void seedDefectType(String defectCode, String defectName) {
        if (defectTypeRepository.findByDefectCode(defectCode).isPresent()) {
            return;
        }
        defectTypeRepository.save(DefectType.builder()
                .defectCode(defectCode)
                .defectName(defectName)
                .build());
    }

    private void seedMeasurementSpecs() {
        seedMeasurementSpec("THICKNESS", "100", "200", "μm");
        seedMeasurementSpec("ALIGNMENT", "-2", "2", "mm");
        seedMeasurementSpec("CURRENT", "500", "1500", "mA");
        seedMeasurementSpec("TORQUE", "5", "15", "N·m");
        seedMeasurementSpec("VOLTAGE", "3.2", "4.2", "V");
        seedMeasurementSpec("CAPACITY", "4.8", "5.2", "Ah");
        seedMeasurementSpec("RESISTANCE", "20", "30", "mΩ");
        seedMeasurementSpec("WEIGHT", "300", "500", "g");
    }

    private void seedMeasurementSpec(String measurementCode, String minValue, String maxValue, String unit) {
        if (measurementSpecRepository.findByMeasurementCode(measurementCode).isPresent()) {
            return;
        }
        DefectType defectType = defectTypeRepository.findByDefectCode(measurementCode)
                .orElseThrow(() -> new IllegalStateException("DefectType 시드 누락: " + measurementCode));
        measurementSpecRepository.save(MeasurementSpec.builder()
                .measurementCode(measurementCode)
                .minValue(new BigDecimal(minValue))
                .maxValue(new BigDecimal(maxValue))
                .unit(unit)
                .defectType(defectType)
                .build());
    }
}
