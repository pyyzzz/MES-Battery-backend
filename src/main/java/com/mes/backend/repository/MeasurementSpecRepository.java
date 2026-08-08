// MeasurementSpec(측정 스펙) 레포지토리
package com.mes.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mes.backend.entity.MeasurementSpec;

public interface MeasurementSpecRepository extends JpaRepository<MeasurementSpec, Long> {
    Optional<MeasurementSpec> findByMeasurementCode(String measurementCode);
}
