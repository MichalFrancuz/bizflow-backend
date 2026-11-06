
package com.crudapp.fullstackbackend.dto.project;

import java.math.BigDecimal;
import java.util.List;

public record ProjectFinancialSummaryResponse(
        Long projectId,
        String projectName,
        BigDecimal budget,
        String budgetCurrency,
        List<CurrencyFinancialSummary> financialSummaries
) {

    public record CurrencyFinancialSummary(
            String currency,
            BigDecimal totalRevenue,
            BigDecimal totalCosts,
            BigDecimal profit,
            BigDecimal profitMarginPercentage
    ) {}
}
