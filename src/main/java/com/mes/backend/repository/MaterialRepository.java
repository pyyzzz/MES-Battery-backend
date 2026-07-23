package com.mes.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.mes.backend.entity.Material;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    Optional<Material> findByCode(String code);

    Optional<Material> findByMaterialCode(String materialCode);

    @Query("""
            select distinct material
            from Material material
            left join fetch material.materialLots materialLot
            order by material.id asc
            """)
    List<Material> findAllForInventoryPage();
}
