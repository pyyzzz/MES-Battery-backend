// Process(공정) 레포지토리
package com.mes.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mes.backend.entity.Process;

public interface ProcessRepository extends JpaRepository<Process, Long> {
    Optional<Process> findByProcessCode(String processCode);
    Optional<Process> findByProcessType(Integer processType);

    @Query("""
            select p from Process p
            where (:processName is null or p.processName like %:processName%)
              and (:processStatus is null or p.processStatus = :processStatus)
            order by p.sequenceNo asc
            """)
    List<Process> search(@Param("processName") String processName, @Param("processStatus") String processStatus);
}