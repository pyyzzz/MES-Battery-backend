// MaterialTransaction(자재 트랜잭션) 레포지토리
package com.mes.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mes.backend.entity.MaterialTransaction;

public interface MaterialTransactionRepository extends JpaRepository<MaterialTransaction, Long> {
}
