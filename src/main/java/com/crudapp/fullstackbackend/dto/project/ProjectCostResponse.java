
package com.crudapp.fullstackbackend.dto.project;

import com.crudapp.fullstackbackend.model.ProjectCost;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectCostResponse(

        Long id,
        ProjectCost.Category category,
        String description,
        BigDecimal amount,
        String currency,
        LocalDate costDate,
        Long projectId,
        LocalDateTime createdAt

) {
}
