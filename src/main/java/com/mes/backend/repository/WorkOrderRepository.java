package com.mes.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mes.backend.entity.WorkOrder;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    Optional<WorkOrder> findFirstByWorkOrderStatus(String workOrderStatus);
    Optional<WorkOrder> findFirstByWorkOrderStatusOrderByIdAsc(String workOrderStatus);
    long countByWorkOrderNoStartingWith(String prefix);

    @Query("""
            select w from WorkOrder w
            where (:workOrderNo is null or w.workOrderNo like %:workOrderNo%)
              and (:workOrderStatus is null or w.workOrderStatus = :workOrderStatus)
              and (:dueDate is null or w.dueDate = :dueDate)
            order by w.id desc
            """)
    List<WorkOrder> search(@Param("workOrderNo") String workOrderNo,
                            @Param("workOrderStatus") String workOrderStatus,
                            @Param("dueDate") LocalDate dueDate);
}
