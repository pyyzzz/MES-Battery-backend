// DefectType(불량유형) 레포지토리
package com.mes.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mes.backend.entity.DefectType;

public interface DefectTypeRepository extends JpaRepository<DefectType, Long> {
    Optional<DefectType> findByDefectCode(String defectCode);
}
