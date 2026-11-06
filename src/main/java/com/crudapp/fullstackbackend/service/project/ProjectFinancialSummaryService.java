
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ProjectFinancialSummaryService {

    private final ProjectRepository projectRepository;
    private final ProjectCostRepository projectCostRepository;
    private final ProjectRevenueRepository projectRevenueRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final OrganizationAccessService organizationAccessService;

    public ProjectFinancialSummaryService(
            ProjectRepository projectRepository,
            ProjectCostRepository projectCostRepository,
            ProjectRevenueRepository projectRevenueRepository,
            TimeEntryRepository timeEntryRepository,
            OrganizationAccessService organizationAccessService
    ) {
        this.projectRepository = projectRepository;
        this.projectCostRepository = projectCostRepository;
        this.projectRevenueRepository = projectRevenueRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.organizationAccessService = organizationAccessService;
    }

    @Transactional(readOnly = true)
    public ProjectFinancialSummaryResponse getFinancialSummary(
            Long organizationId,
            Long projectId
    ) {
        Organization organization =
                organizationAccessService.requireMembership(organizationId);

        Project project = projectRepository.findByIdAndOrganizationId(
                projectId,
                organization.getId()
        ).orElseThrow(() ->
                new EntityNotFoundException("Project not found")
        );

        List<ProjectCost> costs =
                projectCostRepository.findByProjectId(project.getId());

        List<ProjectRevenue> revenues =
                projectRevenueRepository.findByProjectId(project.getId());

        List<TimeEntry> timeEntries =
                timeEntryRepository.findByProjectId(project.getId());

        Map<String, FinancialTotals> totalsByCurrency = new TreeMap<>();

        for (ProjectRevenue revenue : revenues) {
            totalsByCurrency
                    .computeIfAbsent(
                            revenue.getCurrency(),
                            currency -> new FinancialTotals()
                    )
                    .addRevenue(revenue.getAmount());
        }

        for (ProjectCost cost : costs) {
            totalsByCurrency
                    .computeIfAbsent(
                            cost.getCurrency(),
                            currency -> new FinancialTotals()
                    )
                    .addCost(cost.getAmount());
        }

        for (TimeEntry timeEntry : timeEntries) {
            totalsByCurrency
                    .computeIfAbsent(
                            timeEntry.getCurrency(),
                            currency -> new FinancialTotals()
                    )
                    .addCost(timeEntry.getTotalCost());
        }

        List<ProjectFinancialSummaryResponse.CurrencyFinancialSummary>
                financialSummaries = totalsByCurrency.entrySet()
                .stream()
                .map(entry -> {
                    FinancialTotals totals = entry.getValue();

                    BigDecimal profit = totals.totalRevenue
                            .subtract(totals.totalCosts);

                    BigDecimal profitMargin = totals.totalRevenue
                            .compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO.setScale(2)
                            : profit
                            .multiply(BigDecimal.valueOf(100))
                            .divide(
                                    totals.totalRevenue,
                                    2,
                                    RoundingMode.HALF_UP
                            );

                    return new ProjectFinancialSummaryResponse
                            .CurrencyFinancialSummary(
                            entry.getKey(),
                            totals.totalRevenue,
                            totals.totalCosts,
                            profit,
                            profitMargin
                    );
                })
                .toList();

        return new ProjectFinancialSummaryResponse(
                project.getId(),
                project.getName(),
                project.getBudget(),
                project.getCurrency(),
                financialSummaries
        );
    }

    private static class FinancialTotals {

        private BigDecimal totalRevenue = BigDecimal.ZERO;
        private BigDecimal totalCosts = BigDecimal.ZERO;

        private void addRevenue(BigDecimal amount) {
            totalRevenue = totalRevenue.add(amount);
        }

        private void addCost(BigDecimal amount) {
            totalCosts = totalCosts.add(amount);
        }
    }
}
