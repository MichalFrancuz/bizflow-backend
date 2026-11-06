
package com.crudapp.fullstackbackend.service.project;

import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.OrganizationMember.Role;
import com.crudapp.fullstackbackend.model.Project;
import com.crudapp.fullstackbackend.model.Project.Status;
import com.crudapp.fullstackbackend.repository.ProjectRepository;
import com.crudapp.fullstackbackend.service.organization.OrganizationAccessService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class ProjectService {

    private static final Set<Role> PROJECT_WRITE_ROLES =
            Set.of(Role.OWNER, Role.ADMIN, Role.MANAGER);

    private final ProjectRepository projectRepository;
    private final OrganizationAccessService organizationAccessService;

    public ProjectService(
            ProjectRepository projectRepository,
            OrganizationAccessService organizationAccessService
    ) {
        this.projectRepository = projectRepository;
        this.organizationAccessService = organizationAccessService;
    }

    @Transactional(readOnly = true)
    public List<Project> getProjects(Long organizationId) {
        organizationAccessService.requireMembership(organizationId);

        return projectRepository.findByOrganizationId(organizationId);
    }

    @Transactional(readOnly = true)
    public Project getProject(Long organizationId, Long projectId) {
        organizationAccessService.requireMembership(organizationId);

        return findProject(projectId, organizationId);
    }

    @Transactional
    public Project createProject(
            Long organizationId,
            String name,
            String description,
            BigDecimal budget,
            String currency,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Organization organization =
                organizationAccessService.requireRole(
                        organizationId,
                        PROJECT_WRITE_ROLES
                );

        validateProjectData(
                name,
                budget,
                currency,
                startDate,
                endDate
        );

        Project project = new Project();
        project.setName(name.trim());
        project.setDescription(normalizeDescription(description));
        project.setBudget(budget);
        project.setCurrency(currency.trim().toUpperCase());
        project.setStartDate(startDate);
        project.setEndDate(endDate);
        project.setStatus(Status.PLANNED);
        project.setOrganization(organization);

        return projectRepository.save(project);
    }

    @Transactional
    public Project updateProject(
            Long organizationId,
            Long projectId,
            String name,
            String description,
            BigDecimal budget,
            String currency,
            LocalDate startDate,
            LocalDate endDate
    ) {
        organizationAccessService.requireRole(
                organizationId,
                PROJECT_WRITE_ROLES
        );

        validateProjectData(
                name,
                budget,
                currency,
                startDate,
                endDate
        );

        Project project = findProject(projectId, organizationId);

        project.setName(name.trim());
        project.setDescription(normalizeDescription(description));
        project.setBudget(budget);
        project.setCurrency(currency.trim().toUpperCase());
        project.setStartDate(startDate);
        project.setEndDate(endDate);

        return project;
    }

    @Transactional
    public Project updateProjectStatus(
            Long organizationId,
            Long projectId,
            Status status
    ) {
        organizationAccessService.requireRole(
                organizationId,
                PROJECT_WRITE_ROLES
        );

        if (status == null) {
            throw new IllegalArgumentException(
                    "Project status cannot be empty"
            );
        }

        Project project = findProject(projectId, organizationId);
        project.setStatus(status);

        return project;
    }

    @Transactional
    public void deleteProject(Long organizationId, Long projectId) {
        organizationAccessService.requireRole(
                organizationId,
                Set.of(Role.OWNER, Role.ADMIN)
        );

        Project project = findProject(projectId, organizationId);

        projectRepository.delete(project);
    }

    private Project findProject(Long projectId, Long organizationId) {
        return projectRepository.findByIdAndOrganizationId(
                projectId,
                organizationId
        ).orElseThrow(() ->
                new EntityNotFoundException("Project not found")
        );
    }

    private void validateProjectData(
            String name,
            BigDecimal budget,
            String currency,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Project name cannot be empty"
            );
        }

        if (name.trim().length() > 200) {
            throw new IllegalArgumentException(
                    "Project name cannot exceed 200 characters"
            );
        }

        if (budget != null && budget.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Project budget cannot be negative"
            );
        }

        if (currency == null || !currency.trim().matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException(
                    "Currency must be a valid three-letter code"
            );
        }

        if (startDate != null && endDate != null
                && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "Project end date cannot be before start date"
            );
        }
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank()
                ? null
                : description.trim();
    }
}
