// MaterialLot(원료LOT) 레포지토리
package com.mes.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mes.backend.entity.Material;
import com.mes.backend.entity.MaterialLot;

public interface MaterialLotRepository extends JpaRepository<MaterialLot, Long> {
    List<MaterialLot> findAllByMaterialOrderByReceiptDateAsc(Material material);

    @Query("""
            select coalesce(sum(materialLot.currentQuantity), 0)
            from MaterialLot materialLot
            where materialLot.material.id = :materialId
            """)
    java.math.BigDecimal sumCurrentQuantityByMaterialId(@Param("materialId") Long materialId);

    long countByMaterialLotNoStartingWith(String prefix);

    @Query("""
            select distinct materialLot
            from MaterialLot materialLot
            join fetch materialLot.material material
            order by materialLot.receiptDate desc, materialLot.id desc
            """)
    List<MaterialLot> findAllForInventoryLotPage();
}
