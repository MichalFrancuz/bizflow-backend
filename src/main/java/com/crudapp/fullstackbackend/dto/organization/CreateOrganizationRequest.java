
package com.crudapp.fullstackbackend.dto.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(

        @NotBlank(message = "Organization name cannot be empty")
        @Size(max = 200, message = "Organization name cannot exceed 200 characters")
        String name,

        @Size(max = 20, message = "Tax ID cannot exceed 20 characters")
        String taxId

) {
}
