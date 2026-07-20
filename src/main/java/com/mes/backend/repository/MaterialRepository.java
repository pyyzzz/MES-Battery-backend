package com.mes.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mes.backend.entity.Material;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    Optional<Material> findByCode(String code);
}
