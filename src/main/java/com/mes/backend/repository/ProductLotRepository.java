// ProductLot(완제품 LOT) 레포지토리
package com.mes.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mes.backend.entity.ProductLot;

public interface ProductLotRepository extends JpaRepository<ProductLot, Long> {
    long countByProductLotNoStartingWith(String prefix);

    @Query("""
            select distinct productLot
            from ProductLot productLot
            left join fetch productLot.workOrder workOrder
            left join fetch workOrder.bom bom
            left join fetch bom.product product
            where (:productLotNo is null or productLot.productLotNo like %:productLotNo%)
              and (:productName is null or product.productName like %:productName%)
              and (:workOrderNo is null or workOrder.workOrderNo like %:workOrderNo%)
              and (:lotStatus is null or productLot.lotStatus = :lotStatus)
            order by productLot.id desc
            """)
    List<ProductLot> search(@Param("productLotNo") String productLotNo,
                             @Param("productName") String productName,
                             @Param("workOrderNo") String workOrderNo,
                             @Param("lotStatus") String lotStatus);

    @Query("""
            select distinct productLot
            from ProductLot productLot
            left join fetch productLot.workOrder workOrder
            left join fetch workOrder.bom bom
            left join fetch bom.product product
            where productLot.id = :id
            """)
    Optional<ProductLot> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            select distinct productLot
            from ProductLot productLot
            left join fetch productLot.workOrder workOrder
            left join fetch workOrder.managerEmployee manager
            left join fetch workOrder.bom bom
            left join fetch bom.product product
            left join fetch productLot.qualityInspections inspection
            left join fetch inspection.process process
            left join fetch process.managerEmployee processManager
            left join fetch inspection.equipment equipment
            left join fetch inspection.defectType defectType
            left join fetch inspection.inspectorEmployee inspector
            order by productLot.id desc
            """)
    List<ProductLot> findAllForReportPage();
}
