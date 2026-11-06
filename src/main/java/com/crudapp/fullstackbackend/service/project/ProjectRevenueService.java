
package com.crudapp.fullstackbackend.service.project;

import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.OrganizationMember;
import com.crudapp.fullstackbackend.model.Project;
import com.crudapp.fullstackbackend.model.ProjectRevenue;
import com.crudapp.fullstackbackend.repository.ProjectRepository;
import com.crudapp.fullstackbackend.repository.ProjectRevenueRepository;
import com.crudapp.fullstackbackend.service.organization.OrganizationAccessService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ProjectRevenueService {

    private final ProjectRevenueRepository projectRevenueRepository;
    private final ProjectRepository projectRepository;
    private final OrganizationAccessService organizationAccessService;

    public ProjectRevenueService(
            ProjectRevenueRepository projectRevenueRepository,
            ProjectRepository projectRepository,
            OrganizationAccessService organizationAccessService
    ) {
        this.projectRevenueRepository = projectRevenueRepository;
        this.projectRepository = projectRepository;
        this.organizationAccessService = organizationAccessService;
    }

    @Transactional(readOnly = true)
    public List<ProjectRevenue> getRevenues(
            Long organizationId,
            Long projectId
    ) {
        Organization organization =
                organizationAccessService.requireMembership(organizationId);

        Project project = findProject(projectId, organization.getId());

        return projectRevenueRepository.findByProjectId(project.getId());
    }

    public ProjectRevenue createRevenue(
            Long organizationId,
            Long projectId,
            ProjectRevenue revenue
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

        validateRevenue(revenue);

        revenue.setDescription(revenue.getDescription().trim());
        revenue.setCurrency(revenue.getCurrency().toUpperCase());
        revenue.setProject(project);

        return projectRevenueRepository.save(revenue);
    }

    public ProjectRevenue updateRevenue(
            Long organizationId,
            Long projectId,
            Long revenueId,
            ProjectRevenue updatedRevenue
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

        ProjectRevenue existingRevenue = projectRevenueRepository
                .findByIdAndProjectId(revenueId, project.getId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Project revenue not found")
                );

        validateRevenue(updatedRevenue);

        existingRevenue.setCategory(updatedRevenue.getCategory());
        existingRevenue.setDescription(
                updatedRevenue.getDescription().trim()
        );
        existingRevenue.setAmount(updatedRevenue.getAmount());
        existingRevenue.setCurrency(
                updatedRevenue.getCurrency().toUpperCase()
        );
        existingRevenue.setRevenueDate(updatedRevenue.getRevenueDate());

        return projectRevenueRepository.save(existingRevenue);
    }

    public void deleteRevenue(
            Long organizationId,
            Long projectId,
            Long revenueId
    ) {
        Organization organization = organizationAccessService.requireRole(
                organizationId,
                Set.of(
                        OrganizationMember.Role.OWNER,
                        OrganizationMember.Role.ADMIN
                )
        );

        Project project = findProject(projectId, organization.getId());

        ProjectRevenue revenue = projectRevenueRepository
                .findByIdAndProjectId(revenueId, project.getId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Project revenue not found")
                );

        projectRevenueRepository.delete(revenue);
    }

    private Project findProject(Long projectId, Long organizationId) {
        return projectRepository.findByIdAndOrganizationId(
                projectId,
                organizationId
        ).orElseThrow(() ->
                new EntityNotFoundException("Project not found")
        );
    }

    private void validateRevenue(ProjectRevenue revenue) {
        if (revenue == null) {
            throw new IllegalArgumentException("Revenue cannot be null");
        }

        if (revenue.getCategory() == null) {
            throw new IllegalArgumentException("Revenue category is required");
        }

        if (revenue.getDescription() == null
                || revenue.getDescription().isBlank()
                || revenue.getDescription().length() > 1000) {
            throw new IllegalArgumentException(
                    "Revenue description must contain 1 to 1000 characters"
            );
        }

        if (revenue.getAmount() == null
                || revenue.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Revenue amount must be greater than zero"
            );
        }

        if (revenue.getCurrency() == null
                || !revenue.getCurrency().matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException(
                    "Currency must contain exactly three letters"
            );
        }

        if (revenue.getRevenueDate() == null) {
            throw new IllegalArgumentException("Revenue date is required");
        }
    }
}
