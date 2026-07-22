// Equipment(설비) 레포지토리
package com.mes.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mes.backend.entity.Equipment;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
}
