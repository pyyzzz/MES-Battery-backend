// MaterialLot(원료LOT) 레포지토리
package com.mes.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mes.backend.entity.Material;
import com.mes.backend.entity.MaterialLot;

public interface MaterialLotRepository extends JpaRepository<MaterialLot, Long> {
    List<MaterialLot> findAllByMaterialOrderByReceiptDateAsc(Material material);
}
