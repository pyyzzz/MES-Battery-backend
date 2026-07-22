// InspectionMeasurement(검사 측정값) 레포지토리
package com.mes.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mes.backend.entity.InspectionMeasurement;

public interface InspectionMeasurementRepository extends JpaRepository<InspectionMeasurement, Long> {
}
