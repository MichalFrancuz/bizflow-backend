
package com.crudapp.fullstackbackend.dto.organization.member;

import com.crudapp.fullstackbackend.model.OrganizationMember.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddOrganizationMemberRequest(

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Invalid email format")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        String email,

        @NotNull(message = "Role cannot be empty")
        Role role

) {
}
