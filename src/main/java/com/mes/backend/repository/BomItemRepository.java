// BomItem(BOM 소요자재) 레포지토리
package com.mes.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mes.backend.entity.Bom;
import com.mes.backend.entity.BomItem;
import com.mes.backend.entity.Process;

public interface BomItemRepository extends JpaRepository<BomItem, Long> {
    List<BomItem> findAllByBomAndInputProcess(Bom bom, Process inputProcess);
}
