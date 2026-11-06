
package com.crudapp.fullstackbackend.dto.organization.member;

import com.crudapp.fullstackbackend.model.OrganizationMember.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateOrganizationMemberRoleRequest(

        @NotNull(message = "Role cannot be empty")
        Role role

) {
}
