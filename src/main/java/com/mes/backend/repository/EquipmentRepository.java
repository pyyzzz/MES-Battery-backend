// Equipment(설비) 레포지토리
package com.mes.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mes.backend.entity.Equipment;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    Optional<Equipment> findByEquipmentCode(String equipmentCode);
    Optional<Equipment> findByProcess_Id(Long processId);

    @Query("""
            select e from Equipment e
            where (:equipmentName is null or e.equipmentName like %:equipmentName%)
              and (:equipmentStatus is null or e.equipmentStatus = :equipmentStatus)
            order by e.id asc
            """)
    List<Equipment> search(@Param("equipmentName") String equipmentName, @Param("equipmentStatus") String equipmentStatus);
}
