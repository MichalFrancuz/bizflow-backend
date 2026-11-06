
package com.crudapp.fullstackbackend.controller.organization;

import com.crudapp.fullstackbackend.dto.dashboard.OrganizationDashboardResponse;
import com.crudapp.fullstackbackend.service.dashboard.OrganizationDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("bizflow-test")
class OrganizationDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizationDashboardService dashboardService;

    @Test
    void shouldReturnOrganizationDashboard() throws Exception {

        var projectStatistics =
                new OrganizationDashboardResponse.ProjectStatistics(
                        10L,
                        2L,
                        3L,
                        1L,
                        3L,
                        1L
                );

        var employeeStatistics =
                new OrganizationDashboardResponse.EmployeeStatistics(
                        10L,
                        8L,
                        new BigDecimal("150.00")
                );

        var financialSummary =
                new OrganizationDashboardResponse.CurrencyFinancialSummary(
                        "PLN",
                        new BigDecimal("10000.00"),
                        new BigDecimal("3000.00"),
                        new BigDecimal("7000.00"),
                        new BigDecimal("70.00")
                );

        var response = new OrganizationDashboardResponse(
                10L,
                "BizFlow Demo",
                projectStatistics,
                employeeStatistics,
                List.of(financialSummary)
        );

        when(dashboardService.getDashboard(10L))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/api/organizations/10/dashboard"
                ).with(jwt()))
                .andExpect(status().isOk())

                // Organization
                .andExpect(jsonPath("$.organizationId").value(10))
                .andExpect(jsonPath("$.organizationName").value("BizFlow Demo"))

                // Projects
                .andExpect(jsonPath("$.projectStatistics.totalProjects").value(10))
                .andExpect(jsonPath("$.projectStatistics.plannedProjects").value(2))
                .andExpect(jsonPath("$.projectStatistics.activeProjects").value(3))
                .andExpect(jsonPath("$.projectStatistics.onHoldProjects").value(1))
                .andExpect(jsonPath("$.projectStatistics.completedProjects").value(3))
                .andExpect(jsonPath("$.projectStatistics.cancelledProjects").value(1))

                // Employees
                .andExpect(jsonPath("$.employeeStatistics.totalEmployees").value(10))
                .andExpect(jsonPath("$.employeeStatistics.activeEmployees").value(8))
                .andExpect(jsonPath("$.employeeStatistics.totalHoursWorked").value(150.00))

                // Financial summary
                .andExpect(jsonPath("$.financialSummary.length()").value(1))
                .andExpect(jsonPath("$.financialSummary[0].currency").value("PLN"))
                .andExpect(jsonPath("$.financialSummary[0].totalRevenue").value(10000.00))
                .andExpect(jsonPath("$.financialSummary[0].totalCosts").value(3000.00))
                .andExpect(jsonPath("$.financialSummary[0].profit").value(7000.00))
                .andExpect(jsonPath("$.financialSummary[0].profitMarginPercentage").value(70.00));

        verify(dashboardService).getDashboard(10L);
    }

    @Test
    void shouldReturnDashboardWithMultipleCurrencies() throws Exception {

        var eurSummary =
                new OrganizationDashboardResponse.CurrencyFinancialSummary(
                        "EUR",
                        new BigDecimal("2000.00"),
                        new BigDecimal("900.00"),
                        new BigDecimal("1100.00"),
                        new BigDecimal("55.00")
                );

        var plnSummary =
                new OrganizationDashboardResponse.CurrencyFinancialSummary(
                        "PLN",
                        new BigDecimal("10000.00"),
                        new BigDecimal("3000.00"),
                        new BigDecimal("7000.00"),
                        new BigDecimal("70.00")
                );

        var response = new OrganizationDashboardResponse(
                10L,
                "BizFlow Demo",
                new OrganizationDashboardResponse.ProjectStatistics(
                        0, 0, 0, 0, 0, 0
                ),
                new OrganizationDashboardResponse.EmployeeStatistics(
                        0, 0, BigDecimal.ZERO
                ),
                List.of(eurSummary, plnSummary)
        );

        when(dashboardService.getDashboard(10L))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/api/organizations/10/dashboard"
                ).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.financialSummary.length()").value(2))
                .andExpect(jsonPath("$.financialSummary[0].currency").value("EUR"))
                .andExpect(jsonPath("$.financialSummary[0].profit").value(1100.00))
                .andExpect(jsonPath("$.financialSummary[1].currency").value("PLN"))
                .andExpect(jsonPath("$.financialSummary[1].profit").value(7000.00));

        verify(dashboardService).getDashboard(10L);
    }

    @Test
    void shouldReturnEmptyFinancialSummary() throws Exception {

        var response = new OrganizationDashboardResponse(
                10L,
                "BizFlow Demo",
                new OrganizationDashboardResponse.ProjectStatistics(
                        0, 0, 0, 0, 0, 0
                ),
                new OrganizationDashboardResponse.EmployeeStatistics(
                        0, 0, BigDecimal.ZERO
                ),
                List.of()
        );

        when(dashboardService.getDashboard(10L))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/api/organizations/10/dashboard"
                ).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.financialSummary").isArray())
                .andExpect(jsonPath("$.financialSummary.length()").value(0));

        verify(dashboardService).getDashboard(10L);
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {

        mockMvc.perform(get(
                        "/api/organizations/10/dashboard"
                ))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(dashboardService);
    }
}
