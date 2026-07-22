// Product(제품) 레포지토리
package com.mes.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mes.backend.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
