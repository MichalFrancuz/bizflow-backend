package com.crudapp.fullstackbackend.repository;

import com.crudapp.fullstackbackend.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    long countByOrganizationId(Long organizationId);

    long countByOrganizationIdAndActiveTrue(Long organizationId);

    List<Employee> findByOrganizationId(Long organizationId);

    List<Employee> findByOrganizationIdAndActiveTrue(Long organizationId);

    Optional<Employee> findByIdAndOrganizationId(
            Long id,
            Long organizationId
    );

    boolean existsByEmailAndOrganizationId(
            String email,
            Long organizationId
    );
}