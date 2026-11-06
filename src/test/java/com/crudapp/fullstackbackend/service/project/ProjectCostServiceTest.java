
package com.crudapp.fullstackbackend.service.project;

import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.Project;
import com.crudapp.fullstackbackend.model.ProjectCost;
import com.crudapp.fullstackbackend.repository.ProjectCostRepository;
import com.crudapp.fullstackbackend.repository.ProjectRepository;
import com.crudapp.fullstackbackend.service.organization.OrganizationAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectCostServiceTest {

    @Mock
    private ProjectCostRepository projectCostRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private OrganizationAccessService organizationAccessService;

    @InjectMocks
    private ProjectCostService projectCostService;

    private Organization organization;
    private Project project;

    @BeforeEach
    void setUp() {
        organization = mock(Organization.class);
        when(organization.getId()).thenReturn(10L);

        project = mock(Project.class);
    }

    @Test
    void shouldReturnProjectCostsForOrganizationMember() {
        when(project.getId()).thenReturn(20L);

        ProjectCost cost = createValidCost();
        List<ProjectCost> costs = List.of(cost);

        when(organizationAccessService.requireMembership(10L))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        when(projectCostRepository.findByProjectId(20L))
                .thenReturn(costs);

        List<ProjectCost> result =
                projectCostService.getCosts(10L, 20L);

        assertEquals(1, result.size());
        assertSame(cost, result.get(0));

        verify(projectCostRepository).findByProjectId(20L);
    }

    @Test
    void shouldCreateCostForAuthorizedUser() {
        ProjectCost cost = createValidCost();

        when(organizationAccessService.requireRole(eq(10L), anySet()))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        when(projectCostRepository.save(any(ProjectCost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectCost result =
                projectCostService.createCost(10L, 20L, cost);

        assertNotNull(result);
        assertSame(project, result.getProject());
        assertEquals("PLN", result.getCurrency());
        assertEquals("Licencja oprogramowania", result.getDescription());

        verify(projectCostRepository).save(cost);
    }

    @Test
    void shouldRejectManuallyCreatedEmployeeLaborCost() {
        ProjectCost cost = createValidCost();
        cost.setCategory(ProjectCost.Category.EMPLOYEE_LABOR);

        when(organizationAccessService.requireRole(eq(10L), anySet()))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> projectCostService.createCost(10L, 20L, cost)
        );

        assertEquals(
                "Employee labor costs must be registered through time entries",
                exception.getMessage()
        );

        verify(projectCostRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdatingCostToEmployeeLaborCategory() {
        when(project.getId()).thenReturn(20L);

        ProjectCost existingCost = createValidCost();

        ProjectCost updatedCost = createValidCost();
        updatedCost.setCategory(ProjectCost.Category.EMPLOYEE_LABOR);

        when(organizationAccessService.requireRole(eq(10L), anySet()))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        when(projectCostRepository.findByIdAndProjectId(30L, 20L))
                .thenReturn(Optional.of(existingCost));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> projectCostService.updateCost(
                        10L, 20L, 30L, updatedCost
                )
        );

        assertEquals(
                "Employee labor costs must be registered through time entries",
                exception.getMessage()
        );

        verify(projectCostRepository, never()).save(any());
    }

    @Test
    void shouldRejectCostWithZeroAmount() {
        ProjectCost cost = createValidCost();
        cost.setAmount(BigDecimal.ZERO);

        when(organizationAccessService.requireRole(eq(10L), anySet()))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        assertThrows(
                IllegalArgumentException.class,
                () -> projectCostService.createCost(10L, 20L, cost)
        );

        verify(projectCostRepository, never()).save(any());
    }

    @Test
    void shouldRejectCostWithNegativeAmount() {
        ProjectCost cost = createValidCost();
        cost.setAmount(new BigDecimal("-100.00"));

        when(organizationAccessService.requireRole(eq(10L), anySet()))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        assertThrows(
                IllegalArgumentException.class,
                () -> projectCostService.createCost(10L, 20L, cost)
        );

        verify(projectCostRepository, never()).save(any());
    }

    @Test
    void shouldRejectCostWithInvalidCurrency() {
        ProjectCost cost = createValidCost();
        cost.setCurrency("PL");

        when(organizationAccessService.requireRole(eq(10L), anySet()))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        assertThrows(
                IllegalArgumentException.class,
                () -> projectCostService.createCost(10L, 20L, cost)
        );

        verify(projectCostRepository, never()).save(any());
    }

    @Test
    void shouldRejectCostWithBlankDescription() {
        ProjectCost cost = createValidCost();
        cost.setDescription("   ");

        when(organizationAccessService.requireRole(eq(10L), anySet()))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        assertThrows(
                IllegalArgumentException.class,
                () -> projectCostService.createCost(10L, 20L, cost)
        );

        verify(projectCostRepository, never()).save(any());
    }

    @Test
    void shouldRejectCostForProjectFromAnotherOrganization() {
        when(organizationAccessService.requireMembership(10L))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> projectCostService.getCosts(10L, 20L)
        );

        verify(projectCostRepository, never()).findByProjectId(anyLong());
    }

    @Test
    void shouldDeleteCostForAuthorizedOwnerOrAdmin() {
        when(project.getId()).thenReturn(20L);

        ProjectCost cost = createValidCost();

        when(organizationAccessService.requireRole(eq(10L), anySet()))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        when(projectCostRepository.findByIdAndProjectId(30L, 20L))
                .thenReturn(Optional.of(cost));

        projectCostService.deleteCost(10L, 20L, 30L);

        verify(projectCostRepository).delete(cost);
    }

    @Test
    void shouldRejectDeletingCostThatDoesNotBelongToProject() {
        when(project.getId()).thenReturn(20L);

        when(organizationAccessService.requireRole(eq(10L), anySet()))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        when(projectCostRepository.findByIdAndProjectId(30L, 20L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> projectCostService.deleteCost(10L, 20L, 30L)
        );

        verify(projectCostRepository, never()).delete(any());
    }

    private ProjectCost createValidCost() {
        ProjectCost cost = new ProjectCost();
        cost.setCategory(ProjectCost.Category.LICENSES);
        cost.setDescription(" Licencja oprogramowania ");
        cost.setAmount(new BigDecimal("299.99"));
        cost.setCurrency("pln");
        cost.setCostDate(LocalDate.of(2026, 11, 5));
        return cost;
    }
}
