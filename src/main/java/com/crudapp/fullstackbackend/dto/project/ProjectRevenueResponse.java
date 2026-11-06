
package com.crudapp.fullstackbackend.dto.project;

import com.crudapp.fullstackbackend.model.ProjectRevenue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectRevenueResponse(

        Long id,
        ProjectRevenue.Category category,
        String description,
        BigDecimal amount,
        String currency,
        LocalDate revenueDate,
        Long projectId,
        LocalDateTime createdAt

) {
}
