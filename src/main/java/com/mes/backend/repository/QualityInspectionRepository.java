// QualityInspection(품질검사) 레포지토리
package com.mes.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.mes.backend.entity.QualityInspection;

public interface QualityInspectionRepository extends JpaRepository<QualityInspection, Long> {
    @Query("""
            select distinct inspection
            from QualityInspection inspection
            left join fetch inspection.productLot productLot
            left join fetch productLot.workOrder workOrder
            left join fetch workOrder.bom bom
            left join fetch bom.product product
            left join fetch inspection.process proc
            left join fetch inspection.equipment equipment
            left join fetch inspection.defectType defectType
            left join fetch inspection.inspectorEmployee inspector
            left join fetch inspection.inspectionMeasurements measurements
            order by inspection.inspectionAt desc, inspection.id desc
            """)
    List<QualityInspection> findAllForQualityPage();
}
