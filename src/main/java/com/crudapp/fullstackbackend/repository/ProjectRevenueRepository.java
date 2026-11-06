
package com.crudapp.fullstackbackend.repository;

import com.crudapp.fullstackbackend.model.ProjectRevenue;
import com.crudapp.fullstackbackend.model.ProjectRevenue.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRevenueRepository
        extends JpaRepository<ProjectRevenue, Long> {

    List<ProjectRevenue> findByProjectOrganizationId(Long organizationId);

    List<ProjectRevenue> findByProjectId(Long projectId);

    List<ProjectRevenue> findByProjectIdAndCategory(
            Long projectId,
            Category category
    );

    Optional<ProjectRevenue> findByIdAndProjectId(
            Long id,
            Long projectId
    );

    boolean existsByIdAndProjectId(Long id, Long projectId);
}
