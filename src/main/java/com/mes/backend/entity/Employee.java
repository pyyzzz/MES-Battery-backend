package com.mes.backend.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employee")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Long id;

    @Column(name = "employee_no", unique = true, nullable = false)
    private String employeeNo;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "role")
    private String role;

    @Column(name = "is_active")
    private Boolean active;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "employee")
    private List<MaterialTransaction> materialTransactions = new ArrayList<>();

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "managerEmployee")
    private List<WorkOrder> managedWorkOrders = new ArrayList<>();

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "managerEmployee")
    private List<Process> processes = new ArrayList<>();

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "inspectorEmployee")
    private List<QualityInspection> qualityInspections = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (active == null) {
            active = true;
        }
    }
}
