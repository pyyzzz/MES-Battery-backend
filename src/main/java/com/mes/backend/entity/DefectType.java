package com.mes.backend.entity;

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
@Table(name = "defect_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefectType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "defect_type_id")
    private Long id;

    @Column(name = "defect_code", unique = true, nullable = false)
    private String defectCode;

    @Column(name = "defect_name")
    private String defectName;

    @Column(name = "is_active")
    private Boolean active;

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "defectType")
    private List<QualityInspection> qualityInspections = new ArrayList<>();

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "defectType")
    private List<MeasurementSpec> measurementSpecs = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (active == null) {
            active = true;
        }
    }
}
