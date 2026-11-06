
package com.crudapp.fullstackbackend.dto.project;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProjectRequest(

        @NotBlank(message = "Project name cannot be empty")
        @Size(max = 200, message = "Project name cannot exceed 200 characters")
        String name,

        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description,

        @DecimalMin(value = "0.00", message = "Budget cannot be negative")
        BigDecimal budget,

        @NotBlank(message = "Currency cannot be empty")
        @Pattern(
                regexp = "[A-Za-z]{3}",
                message = "Currency must contain exactly three letters"
        )
        String currency,

        LocalDate startDate,

        LocalDate endDate
) {}
