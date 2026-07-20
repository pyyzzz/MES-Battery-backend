package com.mes.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mes.backend.entity.ProductionLog;

public interface ProductionLogRepository extends JpaRepository<ProductionLog, Long> {
    List<ProductionLog> findTop15ByOrderByIdDesc();
}
