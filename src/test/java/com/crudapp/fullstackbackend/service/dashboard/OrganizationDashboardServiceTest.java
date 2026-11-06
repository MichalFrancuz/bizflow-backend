
package com.crudapp.fullstackbackend.service.dashboard;

import com.crudapp.fullstackbackend.dto.dashboard.OrganizationDashboardResponse;
import com.crudapp.fullstackbackend.model.Employee;
import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.Project;
import com.crudapp.fullstackbackend.model.ProjectCost;
import com.crudapp.fullstackbackend.model.ProjectRevenue;
import com.crudapp.fullstackbackend.model.TimeEntry;
import com.crudapp.fullstackbackend.repository.EmployeeRepository;
import com.crudapp.fullstackbackend.repository.ProjectCostRepository;
import com.crudapp.fullstackbackend.repository.ProjectRepository;
import com.crudapp.fullstackbackend.repository.ProjectRevenueRepository;
import com.crudapp.fullstackbackend.repository.TimeEntryRepository;
import com.crudapp.fullstackbackend.service.organization.OrganizationAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationDashboardServiceTest {

    @Mock
    private OrganizationAccessService organizationAccessService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ProjectCostRepository projectCostRepository;

    @Mock
    private ProjectRevenueRepository projectRevenueRepository;

    @Mock
    private TimeEntryRepository timeEntryRepository;

    @InjectMocks
    private OrganizationDashboardService dashboardService;

    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setName("BizFlow Demo");

        ReflectionTestUtils.setField(
                organization,
                "id",
                10L
        );
    }

    @Test
    void shouldCalculateOrganizationDashboard() {
        prepareOrganization();

        prepareProjectStatistics();

        when(employeeRepository.countByOrganizationId(10L))
                .thenReturn(10L);

        when(employeeRepository.countByOrganizationIdAndActiveTrue(10L))
                .thenReturn(8L);

        when(projectRevenueRepository.findByProjectOrganizationId(10L))
                .thenReturn(List.of(
                        createRevenue("10000.00", "PLN"),
                        createRevenue("2000.00", "EUR")
                ));

        when(projectCostRepository.findByProjectOrganizationId(10L))
                .thenReturn(List.of(
                        createCost("2000.00", "PLN"),
                        createCost("500.00", "EUR")
                ));

        when(timeEntryRepository.findByProjectOrganizationId(10L))
                .thenReturn(List.of(
                        createTimeEntry("10.00", "100.00", "PLN"),
                        createTimeEntry("5.00", "80.00", "EUR")
                ));

        OrganizationDashboardResponse result =
                dashboardService.getDashboard(10L);

        // Organization
        assertEquals(10L, result.organizationId());
        assertEquals("BizFlow Demo", result.organizationName());

        // Project statistics
        var projects = result.projectStatistics();

        assertEquals(10L, projects.totalProjects());
        assertEquals(2L, projects.plannedProjects());
        assertEquals(3L, projects.activeProjects());
        assertEquals(1L, projects.onHoldProjects());
        assertEquals(3L, projects.completedProjects());
        assertEquals(1L, projects.cancelledProjects());

        // Employee statistics
        var employees = result.employeeStatistics();

        assertEquals(10L, employees.totalEmployees());
        assertEquals(8L, employees.activeEmployees());
        assertEquals(
                new BigDecimal("15.00"),
                employees.totalHoursWorked()
        );

        // Financial summaries
        assertEquals(2, result.financialSummary().size());

        var eur = result.financialSummary().get(0);

        assertEquals("EUR", eur.currency());
        assertEquals(new BigDecimal("2000.00"), eur.totalRevenue());
        assertEquals(new BigDecimal("900.00"), eur.totalCosts());
        assertEquals(new BigDecimal("1100.00"), eur.profit());
        assertEquals(
                new BigDecimal("55.00"),
                eur.profitMarginPercentage()
        );

        var pln = result.financialSummary().get(1);

        assertEquals("PLN", pln.currency());
        assertEquals(new BigDecimal("10000.00"), pln.totalRevenue());
        assertEquals(new BigDecimal("3000.00"), pln.totalCosts());
        assertEquals(new BigDecimal("7000.00"), pln.profit());
        assertEquals(
                new BigDecimal("70.00"),
                pln.profitMarginPercentage()
        );
    }

    @Test
    void shouldReturnEmptyFinancialSummaryWhenThereAreNoEntries() {
        prepareOrganization();
        prepareProjectStatistics();

        when(employeeRepository.countByOrganizationId(10L))
                .thenReturn(0L);

        when(employeeRepository.countByOrganizationIdAndActiveTrue(10L))
                .thenReturn(0L);

        when(projectRevenueRepository.findByProjectOrganizationId(10L))
                .thenReturn(List.of());

        when(projectCostRepository.findByProjectOrganizationId(10L))
                .thenReturn(List.of());

        when(timeEntryRepository.findByProjectOrganizationId(10L))
                .thenReturn(List.of());

        OrganizationDashboardResponse result =
                dashboardService.getDashboard(10L);

        assertEquals(0L, result.employeeStatistics().totalEmployees());
        assertEquals(0L, result.employeeStatistics().activeEmployees());

        assertEquals(
                BigDecimal.ZERO.setScale(2),
                result.employeeStatistics().totalHoursWorked()
        );

        assertTrue(result.financialSummary().isEmpty());
    }

    @Test
    void shouldReturnZeroMarginWhenThereIsNoRevenue() {
        prepareOrganization();
        prepareProjectStatistics();

        when(employeeRepository.countByOrganizationId(10L))
                .thenReturn(1L);

        when(employeeRepository.countByOrganizationIdAndActiveTrue(10L))
                .thenReturn(1L);

        when(projectRevenueRepository.findByProjectOrganizationId(10L))
                .thenReturn(List.of());

        when(projectCostRepository.findByProjectOrganizationId(10L))
                .thenReturn(List.of(
                        createCost("500.00", "PLN")
                ));

        when(timeEntryRepository.findByProjectOrganizationId(10L))
                .thenReturn(List.of(
                        createTimeEntry("5.00", "100.00", "PLN")
                ));

        OrganizationDashboardResponse result =
                dashboardService.getDashboard(10L);

        var summary = result.financialSummary().getFirst();

        assertEquals("PLN", summary.currency());
        assertEquals(BigDecimal.ZERO, summary.totalRevenue());
        assertEquals(new BigDecimal("1000.00"), summary.totalCosts());
        assertEquals(new BigDecimal("-1000.00"), summary.profit());
        assertEquals(
                new BigDecimal("0.00"),
                summary.profitMarginPercentage()
        );
    }

    @Test
    void shouldCalculateNegativeProfitAndMargin() {
        prepareOrganization();
        prepareProjectStatistics();

        when(employeeRepository.countByOrganizationId(10L))
                .thenReturn(0L);

        when(employeeRepository.countByOrganizationIdAndActiveTrue(10L))
                .thenReturn(0L);

        when(projectRevenueRepository.findByProjectOrganizationId(10L))
                .thenReturn(List.of(
                        createRevenue("1000.00", "PLN")
                ));

        when(projectCostRepository.findByProjectOrganizationId(10L))
                .thenReturn(List.of(
                        createCost("1500.00", "PLN")
                ));

        when(timeEntryRepository.findByProjectOrganizationId(10L))
                .thenReturn(List.of());

        OrganizationDashboardResponse result =
                dashboardService.getDashboard(10L);

        var summary = result.financialSummary().getFirst();

        assertEquals(new BigDecimal("-500.00"), summary.profit());
        assertEquals(
                new BigDecimal("-50.00"),
                summary.profitMarginPercentage()
        );
    }

    @Test
    void shouldRejectDashboardAccessForNonMember() {
        doThrow(new AccessDeniedException("No organization access"))
                .when(organizationAccessService)
                .requireMembership(10L);

        assertThrows(
                AccessDeniedException.class,
                () -> dashboardService.getDashboard(10L)
        );

        verifyNoInteractions(projectRepository);
        verifyNoInteractions(employeeRepository);
        verifyNoInteractions(projectCostRepository);
        verifyNoInteractions(projectRevenueRepository);
        verifyNoInteractions(timeEntryRepository);
    }

    private void prepareOrganization() {
        when(organizationAccessService.requireMembership(10L))
                .thenReturn(organization);
    }

    private void prepareProjectStatistics() {
        when(projectRepository.countByOrganizationIdAndStatus(
                10L,
                Project.Status.PLANNED
        )).thenReturn(2L);

        when(projectRepository.countByOrganizationIdAndStatus(
                10L,
                Project.Status.ACTIVE
        )).thenReturn(3L);

        when(projectRepository.countByOrganizationIdAndStatus(
                10L,
                Project.Status.ON_HOLD
        )).thenReturn(1L);

        when(projectRepository.countByOrganizationIdAndStatus(
                10L,
                Project.Status.COMPLETED
        )).thenReturn(3L);

        when(projectRepository.countByOrganizationIdAndStatus(
                10L,
                Project.Status.CANCELLED
        )).thenReturn(1L);
    }

    private ProjectRevenue createRevenue(
            String amount,
            String currency
    ) {
        ProjectRevenue revenue = new ProjectRevenue();
        revenue.setAmount(new BigDecimal(amount));
        revenue.setCurrency(currency);
        return revenue;
    }

    private ProjectCost createCost(
            String amount,
            String currency
    ) {
        ProjectCost cost = new ProjectCost();
        cost.setAmount(new BigDecimal(amount));
        cost.setCurrency(currency);
        return cost;
    }

    private TimeEntry createTimeEntry(
            String hours,
            String hourlyRate,
            String currency
    ) {
        TimeEntry entry = new TimeEntry();
        entry.setHours(new BigDecimal(hours));
        entry.setHourlyRate(new BigDecimal(hourlyRate));
        entry.setCurrency(currency);
        return entry;
    }
}
