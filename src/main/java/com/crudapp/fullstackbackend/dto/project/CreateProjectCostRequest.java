
package com.crudapp.fullstackbackend.dto.project;

import com.crudapp.fullstackbackend.model.ProjectCost;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProjectCostRequest(

        @NotNull(message = "Cost category is required")
        ProjectCost.Category category,

        @NotBlank(message = "Cost description is required")
        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        @NotNull(message = "Cost amount is required")
        @DecimalMin(value = "0.01", message = "Cost amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(
                regexp = "[A-Za-z]{3}",
                message = "Currency must contain exactly three letters"
        )
        String currency,

        @NotNull(message = "Cost date is required")
        LocalDate costDate

) {
}
