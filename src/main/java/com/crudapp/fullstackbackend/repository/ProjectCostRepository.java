
package com.crudapp.fullstackbackend.repository;

import com.crudapp.fullstackbackend.model.ProjectCost;
import com.crudapp.fullstackbackend.model.ProjectCost.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectCostRepository
        extends JpaRepository<ProjectCost, Long> {

    List<ProjectCost> findByProjectOrganizationId(Long organizationId);

    List<ProjectCost> findByProjectId(Long projectId);

    List<ProjectCost> findByProjectIdAndCategory(
            Long projectId,
            Category category
    );

    Optional<ProjectCost> findByIdAndProjectId(
            Long id,
            Long projectId
    );

    boolean existsByIdAndProjectId(
            Long id,
            Long projectId
    );
}
