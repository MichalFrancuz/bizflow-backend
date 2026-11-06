
package com.crudapp.fullstackbackend.service.project;

import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.OrganizationMember;
import com.crudapp.fullstackbackend.model.Project;
import com.crudapp.fullstackbackend.model.ProjectCost;
import com.crudapp.fullstackbackend.repository.ProjectCostRepository;
import com.crudapp.fullstackbackend.repository.ProjectRepository;
import com.crudapp.fullstackbackend.service.organization.OrganizationAccessService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ProjectCostService {

    private final ProjectCostRepository projectCostRepository;
    private final ProjectRepository projectRepository;
    private final OrganizationAccessService organizationAccessService;

    public ProjectCostService(
            ProjectCostRepository projectCostRepository,
            ProjectRepository projectRepository,
            OrganizationAccessService organizationAccessService
    ) {
        this.projectCostRepository = projectCostRepository;
        this.projectRepository = projectRepository;
        this.organizationAccessService = organizationAccessService;
    }

    @Transactional(readOnly = true)
    public List<ProjectCost> getCosts(Long organizationId, Long projectId) {
        Organization organization =
                organizationAccessService.requireMembership(organizationId);

        Project project = findProject(projectId, organization.getId());

        return projectCostRepository.findByProjectId(project.getId());
    }

    public ProjectCost createCost(
            Long organizationId,
            Long projectId,
            ProjectCost cost
    ) {
        Organization organization = organizationAccessService.requireRole(
                organizationId,
                Set.of(
                        OrganizationMember.Role.OWNER,
                        OrganizationMember.Role.ADMIN,
                        OrganizationMember.Role.MANAGER
                )
        );

        Project project = findProject(projectId, organization.getId());

        validateCost(cost);
        cost.setDescription(cost.getDescription().trim());
        cost.setCurrency(cost.getCurrency().toUpperCase());
        cost.setProject(project);

        return projectCostRepository.save(cost);
    }

    public ProjectCost updateCost(
            Long organizationId,
            Long projectId,
            Long costId,
            ProjectCost updatedCost
    ) {
        Organization organization = organizationAccessService.requireRole(
                organizationId,
                Set.of(
                        OrganizationMember.Role.OWNER,
                        OrganizationMember.Role.ADMIN,
                        OrganizationMember.Role.MANAGER
                )
        );

        Project project = findProject(projectId, organization.getId());

        ProjectCost existingCost = projectCostRepository
                .findByIdAndProjectId(costId, project.getId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Project cost not found")
                );

        validateCost(updatedCost);

        existingCost.setCategory(updatedCost.getCategory());
        existingCost.setDescription(updatedCost.getDescription().trim());
        existingCost.setAmount(updatedCost.getAmount());
        existingCost.setCurrency(updatedCost.getCurrency().toUpperCase());
        existingCost.setCostDate(updatedCost.getCostDate());

        return projectCostRepository.save(existingCost);
    }

    public void deleteCost(
            Long organizationId,
            Long projectId,
            Long costId
    ) {
        Organization organization = organizationAccessService.requireRole(
                organizationId,
                Set.of(
                        OrganizationMember.Role.OWNER,
                        OrganizationMember.Role.ADMIN
                )
        );

        Project project = findProject(projectId, organization.getId());

        ProjectCost cost = projectCostRepository
                .findByIdAndProjectId(costId, project.getId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Project cost not found")
                );

        projectCostRepository.delete(cost);
    }

    private Project findProject(Long projectId, Long organizationId) {
        return projectRepository.findByIdAndOrganizationId(
                projectId,
                organizationId
        ).orElseThrow(() ->
                new EntityNotFoundException("Project not found")
        );
    }

    private void validateCost(ProjectCost cost) {
        if (cost == null) {
            throw new IllegalArgumentException("Cost cannot be null");
        }

        if (cost.getCategory() == null) {
            throw new IllegalArgumentException("Cost category is required");
        }

        if (cost.getCategory() == ProjectCost.Category.EMPLOYEE_LABOR) {
            throw new IllegalArgumentException(
                    "Employee labor costs must be registered through time entries"
            );
        }

        if (cost.getDescription() == null
                || cost.getDescription().isBlank()
                || cost.getDescription().length() > 1000) {
            throw new IllegalArgumentException(
                    "Cost description must contain 1 to 1000 characters"
            );
        }

        if (cost.getAmount() == null
                || cost.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Cost amount must be greater than zero"
            );
        }

        if (cost.getCurrency() == null
                || !cost.getCurrency().matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException(
                    "Currency must contain exactly three letters"
            );
        }

        if (cost.getCostDate() == null) {
            throw new IllegalArgumentException("Cost date is required");
        }
    }
}
