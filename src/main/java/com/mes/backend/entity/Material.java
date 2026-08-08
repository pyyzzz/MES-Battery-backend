package com.mes.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "material")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_id")
    private Long id;

    @Column(name = "material_code", unique = true)
    private String materialCode;

    @Column(name = "material_name")
    private String materialName;

    @Column(name = "unit")
    private String unit;

    @Column(name = "safety_stock", precision = 19, scale = 3)
    private BigDecimal safetyStock;

    @Column(name = "is_active")
    private Boolean active;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "material")
    private List<MaterialLot> materialLots = new ArrayList<>();

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "material")
    private List<BomItem> bomItems = new ArrayList<>();

    @Transient
    private String code;

    @Transient
    private String name;

    @Transient
    private int currentStock;

    @PrePersist
    public void prePersist() {
        if (registeredAt == null) {
            registeredAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
    }
}
