// Employee(계정) 조회용 레포지토리
package com.mes.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mes.backend.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUsername(String username);
    long countByHireDate(LocalDate hireDate);

    @Query("""
            select e from Employee e
            where (:hireDateFrom is null or e.hireDate >= :hireDateFrom)
              and (:hireDateTo is null or e.hireDate <= :hireDateTo)
              and (:role is null or e.role = :role)
            order by e.hireDate desc, e.id desc
            """)
    List<Employee> search(@Param("hireDateFrom") LocalDate hireDateFrom,
                           @Param("hireDateTo") LocalDate hireDateTo,
                           @Param("role") String role);
}
