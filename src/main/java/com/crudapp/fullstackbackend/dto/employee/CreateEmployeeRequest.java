
package com.crudapp.fullstackbackend.dto.employee;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateEmployeeRequest(

        @NotBlank(message = "Employee name cannot be empty")
        @Size(max = 100, message = "Employee name cannot exceed 100 characters")
        String name,

        @Size(max = 100, message = "Family name cannot exceed 100 characters")
        String familyname,

        @Email(message = "Invalid email format")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        String email,

        @Size(max = 30, message = "Phone number cannot exceed 30 characters")
        String phonenumber,

        @DecimalMin(value = "0.0", message = "Salary cannot be negative")
        BigDecimal salary,

        @Pattern(
                regexp = "(?i)^[A-Z]{3}$",
                message = "Currency must contain exactly three letters"
        )
        String salaryCurrency

) {
}
