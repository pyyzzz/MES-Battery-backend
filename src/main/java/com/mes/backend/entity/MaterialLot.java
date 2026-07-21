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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "material_lot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialLot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_lot_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "material_lot_no", unique = true, nullable = false)
    private String materialLotNo;

    @Column(name = "initial_quantity", precision = 19, scale = 3)
    private BigDecimal initialQuantity;

    @Column(name = "current_quantity", precision = 19, scale = 3)
    private BigDecimal currentQuantity;

    @Column(name = "lot_status")
    private String lotStatus;

    @Column(name = "receipt_date")
    private LocalDateTime receiptDate;

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "materialLot")
    private List<MaterialTransaction> materialTransactions = new ArrayList<>();
}
