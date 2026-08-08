package com.mes.backend.entity;

import java.time.LocalDateTime;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quality_inspection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityInspection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inspection_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_lot_id", nullable = false)
    private ProductLot productLot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id", nullable = false)
    private Process process;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defect_type_id")
    private DefectType defectType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_employee_id", nullable = false)
    private Employee inspectorEmployee;

    @Column(name = "inspection_result")
    private String inspectionResult;

    @Column(name = "inspection_at")
    private LocalDateTime inspectionAt;

    @Column(name = "remarks")
    private String remarks;

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "qualityInspection")
    private List<InspectionMeasurement> inspectionMeasurements = new ArrayList<>();
}
