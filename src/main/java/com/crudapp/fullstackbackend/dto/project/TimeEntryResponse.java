
package com.crudapp.fullstackbackend.dto.project;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TimeEntryResponse(
        Long id,
        Long projectId,
        Long employeeId,
        String employeeName,
        String employeeFamilyName,
        String description,
        LocalDate workDate,
        BigDecimal hours,
        BigDecimal hourlyRate,
        String currency,
        BigDecimal totalCost,
        LocalDateTime createdAt
) {}
