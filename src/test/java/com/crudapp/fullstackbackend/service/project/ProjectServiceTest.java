
package com.crudapp.fullstackbackend.service.project;

import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.OrganizationMember.Role;
import com.crudapp.fullstackbackend.model.Project;
import com.crudapp.fullstackbackend.model.Project.Status;
import com.crudapp.fullstackbackend.repository.ProjectRepository;
import com.crudapp.fullstackbackend.service.organization.OrganizationAccessService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private OrganizationAccessService organizationAccessService;

    @InjectMocks
    private ProjectService projectService;

    private Project createProject(Long id, Organization organization) {
        Project project = new Project();
        project.setName("BizFlow implementation");
        project.setStatus(Status.ACTIVE);
        project.setBudget(new BigDecimal("25000.00"));
        project.setCurrency("PLN");
        project.setOrganization(organization);
        return project;
    }

    @Test
    void shouldReturnProjectsForOrganization() {
        Project project = createProject(1L, new Organization());

        when(projectRepository.findByOrganizationId(10L))
                .thenReturn(List.of(project));

        var result = projectService.getProjects(10L);

        assertEquals(1, result.size());
        assertEquals("BizFlow implementation", result.get(0).getName());

        verify(organizationAccessService).requireMembership(10L);
        verify(projectRepository).findByOrganizationId(10L);
    }

    @Test
    void shouldReturnProjectBelongingToOrganization() {
        Organization organization = new Organization();
        Project project = createProject(1L, organization);

        when(projectRepository.findByIdAndOrganizationId(1L, 10L))
                .thenReturn(Optional.of(project));

        Project result = projectService.getProject(10L, 1L);

        assertEquals("BizFlow implementation", result.getName());

        verify(organizationAccessService).requireMembership(10L);
    }

    @Test
    void shouldRejectAccessToProjectFromDifferentOrganization() {
        when(projectRepository.findByIdAndOrganizationId(1L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> projectService.getProject(10L, 1L)
        );

        verify(projectRepository)
                .findByIdAndOrganizationId(1L, 10L);
    }

    @Test
    void shouldCreateProjectSuccessfully() {
        Organization organization = new Organization();

        when(organizationAccessService.requireRole(
                eq(10L),
                anySet()
        )).thenReturn(organization);

        when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Project result = projectService.createProject(
                10L,
                " New project ",
                " Project description ",
                new BigDecimal("15000.00"),
                "pln",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2027, 3, 31)
        );

        assertEquals("New project", result.getName());
        assertEquals("Project description", result.getDescription());
        assertEquals(new BigDecimal("15000.00"), result.getBudget());
        assertEquals("PLN", result.getCurrency());
        assertEquals(Status.PLANNED, result.getStatus());
        assertEquals(organization, result.getOrganization());

        verify(projectRepository).save(any(Project.class));

        verify(organizationAccessService).requireRole(
                eq(10L),
                eq(Set.of(Role.OWNER, Role.ADMIN, Role.MANAGER))
        );
    }

    @Test
    void shouldRejectNegativeProjectBudget() {
        when(organizationAccessService.requireRole(
                eq(10L),
                anySet()
        )).thenReturn(new Organization());

        assertThrows(
                IllegalArgumentException.class,
                () -> projectService.createProject(
                        10L,
                        "Invalid project",
                        null,
                        new BigDecimal("-100.00"),
                        "PLN",
                        null,
                        null
                )
        );

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void shouldRejectInvalidProjectDates() {
        when(organizationAccessService.requireRole(
                eq(10L),
                anySet()
        )).thenReturn(new Organization());

        assertThrows(
                IllegalArgumentException.class,
                () -> projectService.createProject(
                        10L,
                        "Invalid project",
                        null,
                        BigDecimal.ZERO,
                        "PLN",
                        LocalDate.of(2027, 5, 1),
                        LocalDate.of(2027, 3, 1)
                )
        );

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void shouldUpdateProjectStatus() {
        Project project = createProject(1L, new Organization());

        when(projectRepository.findByIdAndOrganizationId(1L, 10L))
                .thenReturn(Optional.of(project));

        Project result = projectService.updateProjectStatus(
                10L,
                1L,
                Status.COMPLETED
        );

        assertEquals(Status.COMPLETED, result.getStatus());

        verify(organizationAccessService).requireRole(
                eq(10L),
                eq(Set.of(Role.OWNER, Role.ADMIN, Role.MANAGER))
        );
    }

    @Test
    void shouldRejectNullProjectStatus() {
        assertThrows(
                IllegalArgumentException.class,
                () -> projectService.updateProjectStatus(10L, 1L, null)
        );

        verifyNoInteractions(projectRepository);
    }

    @Test
    void shouldDeleteProjectOnlyWithOwnerOrAdminPermissions() {
        Project project = createProject(1L, new Organization());

        when(projectRepository.findByIdAndOrganizationId(1L, 10L))
                .thenReturn(Optional.of(project));

        projectService.deleteProject(10L, 1L);

        verify(organizationAccessService).requireRole(
                eq(10L),
                eq(Set.of(Role.OWNER, Role.ADMIN))
        );

        verify(projectRepository).delete(project);
    }

    @Test
    void shouldNotDeleteProjectFromDifferentOrganization() {
        when(projectRepository.findByIdAndOrganizationId(1L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> projectService.deleteProject(10L, 1L)
        );

        verify(projectRepository, never()).delete(any(Project.class));
    }
}
