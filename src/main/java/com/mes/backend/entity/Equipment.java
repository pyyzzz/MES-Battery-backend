package com.mes.backend.entity;

import java.math.BigDecimal;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "equipment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "equipment_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id", unique = true, nullable = false)
    private Process process;

    @Column(name = "equipment_code", unique = true)
    private String equipmentCode;

    @Column(name = "equipment_port", unique = true)
    private Integer equipmentPort;

    @Column(name = "equipment_name")
    private String equipmentName;

    @Column(name = "equipment_status")
    private String equipmentStatus;

    @Column(name = "is_active")
    private Boolean active;

    @Column(name = "status_message")
    private String statusMessage;

    @Column(name = "current_temp", precision = 10, scale = 3)
    private BigDecimal currentTemp;

    @Column(name = "current_humidity", precision = 10, scale = 3)
    private BigDecimal currentHumidity;

    @Column(name = "current_supply_voltage", precision = 10, scale = 3)
    private BigDecimal currentSupplyVoltage;

    @Column(name = "last_battery_voltage", precision = 10, scale = 3)
    private BigDecimal lastBatteryVoltage;

    @Column(name = "last_reported_at")
    private LocalDateTime lastReportedAt;

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "equipment")
    private List<QualityInspection> qualityInspections = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (active == null) {
            active = true;
        }
    }
}
