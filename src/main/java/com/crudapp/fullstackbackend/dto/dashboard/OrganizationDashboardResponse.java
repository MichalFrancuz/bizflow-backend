package com.crudapp.fullstackbackend.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record OrganizationDashboardResponse(
        Long organizationId,
        String organizationName,
        ProjectStatistics projectStatistics,
        EmployeeStatistics employeeStatistics,
        List<CurrencyFinancialSummary> financialSummary
) {

    public record ProjectStatistics(
            long totalProjects,
            long plannedProjects,
            long activeProjects,
            long onHoldProjects,
            long completedProjects,
            long cancelledProjects
    ) {
    }

    public record EmployeeStatistics(
            long totalEmployees,
            long activeEmployees,
            BigDecimal totalHoursWorked
    ) {
    }

    public record CurrencyFinancialSummary(
            String currency,
            BigDecimal totalRevenue,
            BigDecimal totalCosts,
            BigDecimal profit,
            BigDecimal profitMarginPercentage
    ) {
    }
}