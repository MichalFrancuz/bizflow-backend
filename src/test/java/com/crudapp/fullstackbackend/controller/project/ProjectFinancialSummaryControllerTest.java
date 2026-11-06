
package com.crudapp.fullstackbackend.controller.project;

import com.crudapp.fullstackbackend.dto.project.ProjectFinancialSummaryResponse;
import com.crudapp.fullstackbackend.service.project.ProjectFinancialSummaryService;
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
class ProjectFinancialSummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectFinancialSummaryService financialSummaryService;

    @Test
    void shouldReturnProjectFinancialSummary() throws Exception {
        var currencySummary =
                new ProjectFinancialSummaryResponse.CurrencyFinancialSummary(
                        "PLN",
                        new BigDecimal("25000.00"),
                        new BigDecimal("10000.00"),
                        new BigDecimal("15000.00"),
                        new BigDecimal("60.00")
                );

        var response = new ProjectFinancialSummaryResponse(
                20L,
                "Website development",
                new BigDecimal("30000.00"),
                "PLN",
                List.of(currencySummary)
        );

        when(financialSummaryService.getFinancialSummary(10L, 20L))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/api/organizations/10/projects/20/financial-summary"
                ).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(20))
                .andExpect(jsonPath("$.projectName").value("Website development"))
                .andExpect(jsonPath("$.budget").value(30000.00))
                .andExpect(jsonPath("$.budgetCurrency").value("PLN"))
                .andExpect(jsonPath("$.financialSummaries.length()").value(1))
                .andExpect(jsonPath("$.financialSummaries[0].currency").value("PLN"))
                .andExpect(jsonPath("$.financialSummaries[0].totalRevenue").value(25000.00))
                .andExpect(jsonPath("$.financialSummaries[0].totalCosts").value(10000.00))
                .andExpect(jsonPath("$.financialSummaries[0].profit").value(15000.00))
                .andExpect(jsonPath("$.financialSummaries[0].profitMarginPercentage").value(60.00));

        verify(financialSummaryService).getFinancialSummary(10L, 20L);
    }

    @Test
    void shouldReturnFinancialSummaryWithMultipleCurrencies() throws Exception {
        var eurSummary =
                new ProjectFinancialSummaryResponse.CurrencyFinancialSummary(
                        "EUR",
                        new BigDecimal("1000.00"),
                        new BigDecimal("200.00"),
                        new BigDecimal("800.00"),
                        new BigDecimal("80.00")
                );

        var plnSummary =
                new ProjectFinancialSummaryResponse.CurrencyFinancialSummary(
                        "PLN",
                        new BigDecimal("5000.00"),
                        new BigDecimal("1000.00"),
                        new BigDecimal("4000.00"),
                        new BigDecimal("80.00")
                );

        var response = new ProjectFinancialSummaryResponse(
                20L,
                "Website development",
                new BigDecimal("30000.00"),
                "PLN",
                List.of(eurSummary, plnSummary)
        );

        when(financialSummaryService.getFinancialSummary(10L, 20L))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/api/organizations/10/projects/20/financial-summary"
                ).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.financialSummaries.length()").value(2))
                .andExpect(jsonPath("$.financialSummaries[0].currency").value("EUR"))
                .andExpect(jsonPath("$.financialSummaries[0].profit").value(800.00))
                .andExpect(jsonPath("$.financialSummaries[1].currency").value("PLN"))
                .andExpect(jsonPath("$.financialSummaries[1].profit").value(4000.00));

        verify(financialSummaryService).getFinancialSummary(10L, 20L);
    }

    @Test
    void shouldReturnEmptyFinancialSummary() throws Exception {
        var response = new ProjectFinancialSummaryResponse(
                20L,
                "Website development",
                new BigDecimal("30000.00"),
                "PLN",
                List.of()
        );

        when(financialSummaryService.getFinancialSummary(10L, 20L))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/api/organizations/10/projects/20/financial-summary"
                ).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.financialSummaries").isArray())
                .andExpect(jsonPath("$.financialSummaries.length()").value(0));

        verify(financialSummaryService).getFinancialSummary(10L, 20L);
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get(
                        "/api/organizations/10/projects/20/financial-summary"
                ))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(financialSummaryService);
    }
}
