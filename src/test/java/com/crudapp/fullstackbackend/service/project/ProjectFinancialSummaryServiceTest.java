
package com.crudapp.fullstackbackend.service.project;

import com.crudapp.fullstackbackend.dto.project.ProjectFinancialSummaryResponse;
import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.Project;
import com.crudapp.fullstackbackend.model.ProjectCost;
import com.crudapp.fullstackbackend.model.ProjectRevenue;
import com.crudapp.fullstackbackend.model.TimeEntry;
import com.crudapp.fullstackbackend.repository.ProjectCostRepository;
import com.crudapp.fullstackbackend.repository.ProjectRepository;
import com.crudapp.fullstackbackend.repository.ProjectRevenueRepository;
import com.crudapp.fullstackbackend.repository.TimeEntryRepository;
import com.crudapp.fullstackbackend.service.organization.OrganizationAccessService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectFinancialSummaryServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectCostRepository projectCostRepository;

    @Mock
    private ProjectRevenueRepository projectRevenueRepository;

    @Mock
    private TimeEntryRepository timeEntryRepository;

    @Mock
    private OrganizationAccessService organizationAccessService;

    @InjectMocks
    private ProjectFinancialSummaryService financialSummaryService;

    private Organization organization;
    private Project project;

    @BeforeEach
    void setUp() {
        organization = mock(Organization.class);
        when(organization.getId()).thenReturn(10L);

        project = new Project();
        project.setName("Website development");
        project.setCurrency("PLN");
        project.setBudget(new BigDecimal("30000.00"));

        org.springframework.test.util.ReflectionTestUtils
                .setField(project, "id", 20L);
    }

    @Test
    void shouldCalculateProjectProfitAndMargin() {
        prepareProject();

        when(projectRevenueRepository.findByProjectId(20L))
                .thenReturn(List.of(
                        createRevenue("10000.00", "PLN"),
                        createRevenue("15000.00", "PLN")
                ));

        when(projectCostRepository.findByProjectId(20L))
                .thenReturn(List.of(
                        createCost("5000.00", "PLN"),
                        createCost("5000.00", "PLN")
                ));

        when(timeEntryRepository.findByProjectId(20L))
                .thenReturn(List.of());

        ProjectFinancialSummaryResponse result =
                financialSummaryService.getFinancialSummary(10L, 20L);

        assertEquals(20L, result.projectId());
        assertEquals("Website development", result.projectName());
        assertEquals(new BigDecimal("30000.00"), result.budget());
        assertEquals("PLN", result.budgetCurrency());

        assertEquals(1, result.financialSummaries().size());

        var summary = result.financialSummaries().getFirst();

        assertEquals("PLN", summary.currency());
        assertEquals(new BigDecimal("25000.00"), summary.totalRevenue());
        assertEquals(new BigDecimal("10000.00"), summary.totalCosts());
        assertEquals(new BigDecimal("15000.00"), summary.profit());
        assertEquals(new BigDecimal("60.00"), summary.profitMarginPercentage());
    }

    @Test
    void shouldIncludeTimeEntryCostsInProjectProfitability() {
        prepareProject();

        when(projectRevenueRepository.findByProjectId(20L))
                .thenReturn(List.of(
                        createRevenue("10000.00", "PLN")
                ));

        when(projectCostRepository.findByProjectId(20L))
                .thenReturn(List.of(
                        createCost("1000.00", "PLN")
                ));

        when(timeEntryRepository.findByProjectId(20L))
                .thenReturn(List.of(
                        createTimeEntry("8.00", "100.00", "PLN")
                ));

        ProjectFinancialSummaryResponse result =
                financialSummaryService.getFinancialSummary(10L, 20L);

        var summary = result.financialSummaries().getFirst();

        assertEquals("PLN", summary.currency());
        assertEquals(new BigDecimal("10000.00"), summary.totalRevenue());
        assertEquals(new BigDecimal("1800.00"), summary.totalCosts());
        assertEquals(new BigDecimal("8200.00"), summary.profit());
        assertEquals(new BigDecimal("82.00"), summary.profitMarginPercentage());
    }

    @Test
    void shouldCalculateMultipleTimeEntryCosts() {
        prepareProject();

        when(projectRevenueRepository.findByProjectId(20L))
                .thenReturn(List.of(
                        createRevenue("20000.00", "PLN")
                ));

        when(projectCostRepository.findByProjectId(20L))
                .thenReturn(List.of(
                        createCost("2000.00", "PLN")
                ));

        when(timeEntryRepository.findByProjectId(20L))
                .thenReturn(List.of(
                        createTimeEntry("8.00", "100.00", "PLN"),
                        createTimeEntry("5.00", "120.00", "PLN")
                ));

        ProjectFinancialSummaryResponse result =
                financialSummaryService.getFinancialSummary(10L, 20L);

        var summary = result.financialSummaries().getFirst();

        // 8 * 100 = 800
        // 5 * 120 = 600
        // 800 + 600 + 2000 = 3400
        assertEquals(new BigDecimal("3400.00"), summary.totalCosts());
        assertEquals(new BigDecimal("16600.00"), summary.profit());
    }

    @Test
    void shouldSeparateTimeEntryCostsByCurrency() {
        prepareProject();

        when(projectRevenueRepository.findByProjectId(20L))
                .thenReturn(List.of(
                        createRevenue("1000.00", "EUR"),
                        createRevenue("5000.00", "PLN")
                ));

        when(projectCostRepository.findByProjectId(20L))
                .thenReturn(List.of(
                        createCost("200.00", "EUR"),
                        createCost("1000.00", "PLN")
                ));

        when(timeEntryRepository.findByProjectId(20L))
                .thenReturn(List.of(
                        createTimeEntry("2.00", "100.00", "EUR"),
                        createTimeEntry("5.00", "100.00", "PLN")
                ));

        ProjectFinancialSummaryResponse result =
                financialSummaryService.getFinancialSummary(10L, 20L);

        assertEquals(2, result.financialSummaries().size());

        var eurSummary = result.financialSummaries().get(0);
        var plnSummary = result.financialSummaries().get(1);

        assertEquals("EUR", eurSummary.currency());
        assertEquals(new BigDecimal("400.00"), eurSummary.totalCosts());
        assertEquals(new BigDecimal("600.00"), eurSummary.profit());

        assertEquals("PLN", plnSummary.currency());
        assertEquals(new BigDecimal("1500.00"), plnSummary.totalCosts());
        assertEquals(new BigDecimal("3500.00"), plnSummary.profit());
    }

    @Test
    void shouldSeparateDifferentCurrencies() {
        prepareProject();

        when(projectRevenueRepository.findByProjectId(20L))
                .thenReturn(List.of(
                        createRevenue("1000.00", "EUR"),
                        createRevenue("5000.00", "PLN")
                ));

        when(projectCostRepository.findByProjectId(20L))
                .thenReturn(List.of(
                        createCost("200.00", "EUR"),
                        createCost("1000.00", "PLN")
                ));

        when(timeEntryRepository.findByProjectId(20L))
                .thenReturn(List.of());

        ProjectFinancialSummaryResponse result =
                financialSummaryService.getFinancialSummary(10L, 20L);

        assertEquals(2, result.financialSummaries().size());

        var eurSummary = result.financialSummaries().get(0);
        var plnSummary = result.financialSummaries().get(1);

        assertEquals("EUR", eurSummary.currency());
        assertEquals(new BigDecimal("800.00"), eurSummary.profit());

        assertEquals("PLN", plnSummary.currency());
        assertEquals(new BigDecimal("4000.00"), plnSummary.profit());
    }

    @Test
    void shouldCalculateNegativeProfit() {
        prepareProject();

        when(projectRevenueRepository.findByProjectId(20L))
                .thenReturn(List.of(createRevenue("1000.00", "PLN")));

        when(projectCostRepository.findByProjectId(20L))
                .thenReturn(List.of(createCost("1500.00", "PLN")));

        when(timeEntryRepository.findByProjectId(20L))
                .thenReturn(List.of());

        ProjectFinancialSummaryResponse result =
                financialSummaryService.getFinancialSummary(10L, 20L);

        var summary = result.financialSummaries().getFirst();

        assertEquals(new BigDecimal("-500.00"), summary.profit());
        assertEquals(new BigDecimal("-50.00"), summary.profitMarginPercentage());
    }

    @Test
    void shouldReturnZeroMarginWhenThereIsNoRevenue() {
        prepareProject();

        when(projectRevenueRepository.findByProjectId(20L))
                .thenReturn(List.of());

        when(projectCostRepository.findByProjectId(20L))
                .thenReturn(List.of(createCost("500.00", "PLN")));

        when(timeEntryRepository.findByProjectId(20L))
                .thenReturn(List.of(
                        createTimeEntry("5.00", "100.00", "PLN")
                ));

        ProjectFinancialSummaryResponse result =
                financialSummaryService.getFinancialSummary(10L, 20L);

        var summary = result.financialSummaries().getFirst();

        assertEquals(BigDecimal.ZERO, summary.totalRevenue());
        assertEquals(new BigDecimal("1000.00"), summary.totalCosts());
        assertEquals(new BigDecimal("-1000.00"), summary.profit());
        assertEquals(new BigDecimal("0.00"), summary.profitMarginPercentage());
    }

    @Test
    void shouldReturnEmptySummaryWhenThereAreNoFinancialEntries() {
        prepareProject();

        when(projectRevenueRepository.findByProjectId(20L))
                .thenReturn(List.of());

        when(projectCostRepository.findByProjectId(20L))
                .thenReturn(List.of());

        when(timeEntryRepository.findByProjectId(20L))
                .thenReturn(List.of());

        ProjectFinancialSummaryResponse result =
                financialSummaryService.getFinancialSummary(10L, 20L);

        assertTrue(result.financialSummaries().isEmpty());
    }

    @Test
    void shouldRejectProjectThatDoesNotBelongToOrganization() {
        when(organizationAccessService.requireMembership(10L))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> financialSummaryService.getFinancialSummary(10L, 20L)
        );

        verifyNoInteractions(projectCostRepository);
        verifyNoInteractions(projectRevenueRepository);
        verifyNoInteractions(timeEntryRepository);
    }

    private void prepareProject() {
        when(organizationAccessService.requireMembership(10L))
                .thenReturn(organization);

        when(projectRepository.findByIdAndOrganizationId(20L, 10L))
                .thenReturn(Optional.of(project));
    }

    private ProjectRevenue createRevenue(String amount, String currency) {
        ProjectRevenue revenue = new ProjectRevenue();
        revenue.setAmount(new BigDecimal(amount));
        revenue.setCurrency(currency);
        return revenue;
    }

    private ProjectCost createCost(String amount, String currency) {
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
