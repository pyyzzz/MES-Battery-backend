// ProductLot(완제품 LOT) 레포지토리
package com.mes.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mes.backend.entity.ProductLot;

public interface ProductLotRepository extends JpaRepository<ProductLot, Long> {
    long countByProductLotNoStartingWith(String prefix);
}
