package com.mes.backend.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "work_order")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WorkOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_id")
    private Bom bom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_employee_id")
    private Employee managerEmployee;

    @Column(name = "work_order_no", unique = true)
    private String workOrderNo;

    @Column(name = "order_quantity")
    private Integer orderQuantity;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "work_order_status")
    private String workOrderStatus;

    @Column(name = "actual_start_at")
    private LocalDateTime actualStartAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @JsonIgnore
    @OneToOne(mappedBy = "workOrder")
    private ProductLot productLot;

    @Transient
    private String productCode;

    @Transient
    private int targetQty;

    @Transient
    private int currentQty;

    @Transient
    private String status;

    @Transient
    private String assignedMachineId;

    @Transient
    private LocalDateTime createdAt;
}
