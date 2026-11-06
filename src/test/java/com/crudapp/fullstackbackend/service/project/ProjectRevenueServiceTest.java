
package com.crudapp.fullstackbackend.service.project;

import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.Project;
import com.crudapp.fullstackbackend.model.ProjectRevenue;
import com.crudapp.fullstackbackend.repository.ProjectRepository;
import com.crudapp.fullstackbackend.repository.ProjectRevenueRepository;
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
class ProjectRevenueServiceTest {

    @Mock
    private ProjectRevenueRepository projectRevenueRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private OrganizationAccessService organizationAccessService;

    @InjectMocks
    private ProjectRevenueService projectRevenueService;

    private Organization organization;
    private Project project;

    @BeforeEach
    void setUp() {
        organization = mock(Organization.class);
        when(organization.getId()).thenReturn(10L);

        project = mock(Project.class);
    }

    @Test
    void shouldReturnProjectRevenuesForOrganizationMember() {
        when(project.getId()).thenReturn(20L);

        ProjectRevenue revenue = createValidRevenue();

        when(organizationAccessService.requireMembership(10L))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        when(projectRevenueRepository.findByProjectId(20L))
                .thenReturn(List.of(revenue));

        List<ProjectRevenue> result =
                projectRevenueService.getRevenues(10L, 20L);

        assertEquals(1, result.size());
        assertSame(revenue, result.get(0));

        verify(projectRevenueRepository).findByProjectId(20L);
    }

    @Test
    void shouldCreateRevenueForAuthorizedUser() {
        ProjectRevenue revenue = createValidRevenue();

        when(organizationAccessService.requireRole(eq(10L), anySet()))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        when(projectRevenueRepository.save(any(ProjectRevenue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectRevenue result =
                projectRevenueService.createRevenue(10L, 20L, revenue);

        assertNotNull(result);
        assertSame(project, result.getProject());
        assertEquals("PLN", result.getCurrency());
        assertEquals("Płatność od klienta", result.getDescription());

        verify(projectRevenueRepository).save(revenue);
    }

    @Test
    void shouldRejectRevenueWithZeroAmount() {
        ProjectRevenue revenue = createValidRevenue();
        revenue.setAmount(BigDecimal.ZERO);

        when(organizationAccessService.requireRole(eq(10L), anySet()))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        assertThrows(
                IllegalArgumentException.class,
                () -> projectRevenueService.createRevenue(10L, 20L, revenue)
        );

        verify(projectRevenueRepository, never()).save(any());
    }

    @Test
    void shouldRejectRevenueWithNegativeAmount() {
        ProjectRevenue revenue = createValidRevenue();
        revenue.setAmount(new BigDecimal("-100.00"));

        when(organizationAccessService.requireRole(eq(10L), anySet()))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        assertThrows(
                IllegalArgumentException.class,
                () -> projectRevenueService.createRevenue(10L, 20L, revenue)
        );

        verify(projectRevenueRepository, never()).save(any());
    }

    @Test
    void shouldRejectRevenueWithInvalidCurrency() {
        ProjectRevenue revenue = createValidRevenue();
        revenue.setCurrency("PL");

        when(organizationAccessService.requireRole(eq(10L), anySet()))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        assertThrows(
                IllegalArgumentException.class,
                () -> projectRevenueService.createRevenue(10L, 20L, revenue)
        );

        verify(projectRevenueRepository, never()).save(any());
    }

    @Test
    void shouldRejectRevenueWithBlankDescription() {
        ProjectRevenue revenue = createValidRevenue();
        revenue.setDescription("   ");

        when(organizationAccessService.requireRole(eq(10L), anySet()))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        assertThrows(
                IllegalArgumentException.class,
                () -> projectRevenueService.createRevenue(10L, 20L, revenue)
        );

        verify(projectRevenueRepository, never()).save(any());
    }

    @Test
    void shouldRejectRevenueForProjectFromAnotherOrganization() {
        when(organizationAccessService.requireMembership(10L))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> projectRevenueService.getRevenues(10L, 20L)
        );

        verify(projectRevenueRepository, never()).findByProjectId(anyLong());
    }

    @Test
    void shouldDeleteRevenueForAuthorizedOwnerOrAdmin() {
        when(project.getId()).thenReturn(20L);

        ProjectRevenue revenue = createValidRevenue();

        when(organizationAccessService.requireRole(eq(10L), anySet()))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        when(projectRevenueRepository.findByIdAndProjectId(30L, 20L))
                .thenReturn(Optional.of(revenue));

        projectRevenueService.deleteRevenue(10L, 20L, 30L);

        verify(projectRevenueRepository).delete(revenue);
    }

    @Test
    void shouldRejectDeletingRevenueThatDoesNotBelongToProject() {
        when(project.getId()).thenReturn(20L);

        when(organizationAccessService.requireRole(eq(10L), anySet()))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));

        when(projectRevenueRepository.findByIdAndProjectId(30L, 20L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> projectRevenueService.deleteRevenue(10L, 20L, 30L)
        );

        verify(projectRevenueRepository, never()).delete(any());
    }

    private ProjectRevenue createValidRevenue() {
        ProjectRevenue revenue = new ProjectRevenue();
        revenue.setCategory(ProjectRevenue.Category.CLIENT_PAYMENT);
        revenue.setDescription(" Płatność od klienta ");
        revenue.setAmount(new BigDecimal("5000.00"));
        revenue.setCurrency("pln");
        revenue.setRevenueDate(LocalDate.of(2026, 11, 5));

        return revenue;
    }
}
