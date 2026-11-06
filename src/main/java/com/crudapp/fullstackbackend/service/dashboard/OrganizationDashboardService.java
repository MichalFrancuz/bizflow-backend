package com.crudapp.fullstackbackend.service.dashboard;

import com.crudapp.fullstackbackend.dto.dashboard.OrganizationDashboardResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class OrganizationDashboardService {

    private final OrganizationAccessService organizationAccessService;
    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectCostRepository projectCostRepository;
    private final ProjectRevenueRepository projectRevenueRepository;
    private final TimeEntryRepository timeEntryRepository;

    public OrganizationDashboardService(
            OrganizationAccessService organizationAccessService,
            ProjectRepository projectRepository,
            EmployeeRepository employeeRepository,
            ProjectCostRepository projectCostRepository,
            ProjectRevenueRepository projectRevenueRepository,
            TimeEntryRepository timeEntryRepository
    ) {
        this.organizationAccessService = organizationAccessService;
        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
        this.projectCostRepository = projectCostRepository;
        this.projectRevenueRepository = projectRevenueRepository;
        this.timeEntryRepository = timeEntryRepository;
    }

    @Transactional(readOnly = true)
    public OrganizationDashboardResponse getDashboard(Long organizationId) {

        Organization organization =
                organizationAccessService.requireMembership(organizationId);

        // Project statistics
        long plannedProjects = countProjects(
                organizationId,
                Project.Status.PLANNED
        );

        long activeProjects = countProjects(
                organizationId,
                Project.Status.ACTIVE
        );

        long onHoldProjects = countProjects(
                organizationId,
                Project.Status.ON_HOLD
        );

        long completedProjects = countProjects(
                organizationId,
                Project.Status.COMPLETED
        );

        long cancelledProjects = countProjects(
                organizationId,
                Project.Status.CANCELLED
        );

        long totalProjects = plannedProjects
                + activeProjects
                + onHoldProjects
                + completedProjects
                + cancelledProjects;

        OrganizationDashboardResponse.ProjectStatistics projectStatistics =
                new OrganizationDashboardResponse.ProjectStatistics(
                        totalProjects,
                        plannedProjects,
                        activeProjects,
                        onHoldProjects,
                        completedProjects,
                        cancelledProjects
                );

        // Employee statistics
        long totalEmployees =
                employeeRepository.countByOrganizationId(organizationId);

        long activeEmployees =
                employeeRepository.countByOrganizationIdAndActiveTrue(
                        organizationId
                );

        // Financial data and working hours
        List<ProjectCost> costs =
                projectCostRepository.findByProjectOrganizationId(
                        organizationId
                );

        List<ProjectRevenue> revenues =
                projectRevenueRepository.findByProjectOrganizationId(
                        organizationId
                );

        List<TimeEntry> timeEntries =
                timeEntryRepository.findByProjectOrganizationId(
                        organizationId
                );

        Map<String, FinancialTotals> totalsByCurrency = new TreeMap<>();

        BigDecimal totalHoursWorked = BigDecimal.ZERO;

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

            totalHoursWorked = totalHoursWorked.add(
                    timeEntry.getHours()
            );

            totalsByCurrency
                    .computeIfAbsent(
                            timeEntry.getCurrency(),
                            currency -> new FinancialTotals()
                    )
                    .addCost(timeEntry.getTotalCost());
        }

        OrganizationDashboardResponse.EmployeeStatistics employeeStatistics =
                new OrganizationDashboardResponse.EmployeeStatistics(
                        totalEmployees,
                        activeEmployees,
                        totalHoursWorked.setScale(2, RoundingMode.HALF_UP)
                );

        // Financial summaries grouped by currency
        List<OrganizationDashboardResponse.CurrencyFinancialSummary>
                financialSummary = totalsByCurrency.entrySet()
                .stream()
                .map(entry -> {

                    FinancialTotals totals = entry.getValue();

                    BigDecimal profit = totals.totalRevenue
                            .subtract(totals.totalCosts);

                    BigDecimal profitMargin =
                            totals.totalRevenue.compareTo(BigDecimal.ZERO) == 0
                                    ? BigDecimal.ZERO.setScale(2)
                                    : profit
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(
                                            totals.totalRevenue,
                                            2,
                                            RoundingMode.HALF_UP
                                    );

                    return new OrganizationDashboardResponse
                            .CurrencyFinancialSummary(
                            entry.getKey(),
                            totals.totalRevenue,
                            totals.totalCosts,
                            profit,
                            profitMargin
                    );
                })
                .toList();

        return new OrganizationDashboardResponse(
                organization.getId(),
                organization.getName(),
                projectStatistics,
                employeeStatistics,
                financialSummary
        );
    }

    private long countProjects(
            Long organizationId,
            Project.Status status
    ) {
        return projectRepository.countByOrganizationIdAndStatus(
                organizationId,
                status
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