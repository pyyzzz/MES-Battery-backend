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

    /* 유닛 하나의 최종 양품 판정은 8개 측정값이 전부 스펙 안에 들어야 하므로(유닛 단위 최종판정),
     * 필드별 NG율이 곱으로 누적된다. 목표 최종 양품률 ~95% 기준으로 역산: 이산값 종류가 적은
     * ALIGNMENT(5종)/TORQUE·RESISTANCE(11종)는 조금만 좁혀도 NG율이 18~40%로 튀어 95% 목표와
     * 양립 불가 → C 생성 범위와 동일하게 되돌려 NG 0%로 두고, 나머지 5개 필드(값 종류가 많아
     * 세밀한 조절 가능)만 필드당 NG~1%로 좁혀서 전체 통과율 0.99^5 ≈ 95%를 맞춤. */
    private void seedMeasurementSpecs() {
        seedMeasurementSpec("THICKNESS", "100", "199", "μm");
        seedMeasurementSpec("ALIGNMENT", "-2", "2", "mm");
        seedMeasurementSpec("CURRENT", "505", "1495", "mA");
        seedMeasurementSpec("TORQUE", "5", "15", "N·m");
        seedMeasurementSpec("VOLTAGE", "3.20", "4.19", "V");
        seedMeasurementSpec("CAPACITY", "4.802", "5.198", "Ah");
        seedMeasurementSpec("RESISTANCE", "20", "30", "mΩ");
        seedMeasurementSpec("WEIGHT", "301", "499", "g");
    }

    /* 이미 존재하는 row의 min/max가 위 값과 다르면 재기동 시 자동으로 갱신해준다(자가치유) */
    private void seedMeasurementSpec(String measurementCode, String minValue, String maxValue, String unit) {
        BigDecimal min = new BigDecimal(minValue);
        BigDecimal max = new BigDecimal(maxValue);

        MeasurementSpec existing = measurementSpecRepository.findByMeasurementCode(measurementCode).orElse(null);
        if (existing != null) {
            if (existing.getMinValue().compareTo(min) != 0 || existing.getMaxValue().compareTo(max) != 0) {
                existing.setMinValue(min);
                existing.setMaxValue(max);
                measurementSpecRepository.save(existing);
            }
            return;
        }

        DefectType defectType = defectTypeRepository.findByDefectCode(measurementCode)
                .orElseThrow(() -> new IllegalStateException("DefectType 시드 누락: " + measurementCode));
        measurementSpecRepository.save(MeasurementSpec.builder()
                .measurementCode(measurementCode)
                .minValue(min)
                .maxValue(max)
                .unit(unit)
                .defectType(defectType)
                .build());
    }
}
