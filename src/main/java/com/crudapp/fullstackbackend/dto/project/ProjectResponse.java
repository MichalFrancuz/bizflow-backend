
package com.crudapp.fullstackbackend.dto.project;

import com.crudapp.fullstackbackend.model.Project.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        Status status,
        BigDecimal budget,
        String currency,
        LocalDate startDate,
        LocalDate endDate,
        Long organizationId,
        LocalDateTime createdAt
) {}
