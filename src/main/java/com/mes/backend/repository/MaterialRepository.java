// Material(자재) 레포지토리
package com.mes.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mes.backend.entity.Material;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    Optional<Material> findByMaterialCode(String materialCode);

    @Query("""
            select m from Material m
            where (:materialName is null or m.materialName like %:materialName%)
              and (:materialCode is null or m.materialCode like %:materialCode%)
            order by m.id desc
            """)
    List<Material> search(@Param("materialName") String materialName, @Param("materialCode") String materialCode);

    @Query("""
            select distinct material
            from Material material
            left join fetch material.materialLots materialLot
            order by material.id asc
            """)
    List<Material> findAllForInventoryPage();
}
