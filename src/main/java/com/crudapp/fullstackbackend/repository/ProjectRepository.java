
package com.crudapp.fullstackbackend.repository;

import com.crudapp.fullstackbackend.model.Project;
import com.crudapp.fullstackbackend.model.Project.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    long countByOrganizationIdAndStatus(
            Long organizationId,
            Project.Status status
    );

    List<Project> findByOrganizationId(Long organizationId);

    List<Project> findByOrganizationIdAndStatus(
            Long organizationId,
            Status status
    );

    Optional<Project> findByIdAndOrganizationId(
            Long id,
            Long organizationId
    );

    boolean existsByIdAndOrganizationId(
            Long id,
            Long organizationId
    );
}
