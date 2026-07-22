// QualityInspection(품질검사) 레포지토리
package com.mes.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mes.backend.entity.QualityInspection;

public interface QualityInspectionRepository extends JpaRepository<QualityInspection, Long> {
}
