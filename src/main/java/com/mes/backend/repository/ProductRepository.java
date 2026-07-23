// Product(제품) 레포지토리
package com.mes.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mes.backend.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("""
            select p from Product p
            where (:productName is null or p.productName like %:productName%)
              and (:createdFrom is null or p.createdAt >= :createdFrom)
              and (:createdToExclusive is null or p.createdAt < :createdToExclusive)
            order by p.id desc
            """)
    List<Product> search(@Param("productName") String productName,
                          @Param("createdFrom") LocalDateTime createdFrom,
                          @Param("createdToExclusive") LocalDateTime createdToExclusive);
}
