// MaterialTransaction(자재 트랜잭션) 레포지토리
package com.mes.backend.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mes.backend.entity.MaterialTransaction;

public interface MaterialTransactionRepository extends JpaRepository<MaterialTransaction, Long> {
    @Query("""
            select transaction
            from MaterialTransaction transaction
            join fetch transaction.materialLot materialLot
            join fetch materialLot.material material
            left join fetch transaction.productLot productLot
            left join fetch transaction.employee employee
            left join fetch transaction.process process
            order by transaction.transactionAt desc, transaction.id desc
            """)
    List<MaterialTransaction> findAllForInventoryTransactionPage();

    @Query("""
            select transaction
            from MaterialTransaction transaction
            join fetch transaction.productLot productLot
            join fetch transaction.materialLot materialLot
            join fetch materialLot.material material
            left join fetch transaction.process process
            where productLot.id in :productLotIds
              and transaction.transactionType = 'CONSUME'
            order by transaction.transactionAt asc, transaction.id asc
            """)
    List<MaterialTransaction> findConsumeTransactionsForProductLots(@Param("productLotIds") Collection<Long> productLotIds);
}
