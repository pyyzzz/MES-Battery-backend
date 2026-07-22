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
@Table(name = "product_lot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductLot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_lot_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", unique = true, nullable = false)
    private WorkOrder workOrder;

    @Column(name = "product_lot_no", unique = true, nullable = false)
    private String productLotNo;

    @Builder.Default
    @Column(name = "current_qty")
    private Integer currentQty = 0;

    @Column(name = "lot_status")
    private String lotStatus;

    @Column(name = "lot_created_at")
    private LocalDateTime lotCreatedAt;

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "productLot")
    private List<MaterialTransaction> materialTransactions = new ArrayList<>();

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "productLot")
    private List<QualityInspection> qualityInspections = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (lotCreatedAt == null) {
            lotCreatedAt = LocalDateTime.now();
        }
    }
}
