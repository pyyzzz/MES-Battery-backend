package com.mes.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mes.backend.entity.Bom;

public interface BomRepository extends JpaRepository<Bom, Long> {
    List<Bom> findAllByProduct_ProductCode(String productCode);
}
