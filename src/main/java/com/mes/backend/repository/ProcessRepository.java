// Process(공정) 레포지토리
package com.mes.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mes.backend.entity.Process;

public interface ProcessRepository extends JpaRepository<Process, Long> {
    Optional<Process> findByProcessCode(String processCode);
    Optional<Process> findByProcessType(Integer processType);
}