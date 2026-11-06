package com.crudapp.fullstackbackend.dto.project;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTimeEntryRequest(

        @NotNull(message = "Employee ID is required")
        Long employeeId,

        @NotBlank(message = "Description is required")
        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        @NotNull(message = "Work date is required")
        LocalDate workDate,

        @NotNull(message = "Working hours are required")
        @DecimalMin(value = "0.01", message = "Working hours must be greater than zero")
        @DecimalMax(value = "24.00", message = "Working hours cannot exceed 24 hours")
        @Digits(integer = 6, fraction = 2, message = "Working hours must have at most two decimal places")
        BigDecimal hours,

        @NotNull(message = "Hourly rate is required")
        @DecimalMin(value = "0.00", message = "Hourly rate cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "Hourly rate must have at most two decimal places")
        BigDecimal hourlyRate,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "[A-Za-z]{3}", message = "Currency must contain exactly three letters")
        String currency
) {}