package com.mes.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mes.backend.entity.WorkOrder;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findAllByOrderByIdDesc();
    Optional<WorkOrder> findFirstByStatusOrderByIdAsc(String status);
    Optional<WorkOrder> findByStatusAndAssignedMachineId(String status, String machineId);
}
