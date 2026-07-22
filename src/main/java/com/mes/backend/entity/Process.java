package com.mes.backend.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "process")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Process {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "process_id")
    private Long id;

    @Column(name = "process_code", unique = true, nullable = false)
    private String processCode;

    @Column(name = "process_type", unique = true)
    private Integer processType;

    @Column(name = "process_name")
    private String processName;

    @Column(name = "sequence_no")
    private Integer sequenceNo;

    @Column(name = "process_status")
    private String processStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_employee_id", nullable = false)
    private Employee managerEmployee;

    @Column(name = "description")
    private String description;

    @JsonIgnore
    @OneToOne(mappedBy = "process")
    private Equipment equipment;

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "process")
    private List<MaterialTransaction> materialTransactions = new ArrayList<>();

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "inputProcess")
    private List<BomItem> bomItems = new ArrayList<>();

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "process")
    private List<QualityInspection> qualityInspections = new ArrayList<>();
}
