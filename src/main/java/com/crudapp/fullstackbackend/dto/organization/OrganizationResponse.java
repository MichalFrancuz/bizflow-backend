
package com.crudapp.fullstackbackend.dto.organization;

import com.crudapp.fullstackbackend.model.OrganizationMember;

public record OrganizationResponse(
        Long id,
        String name,
        String taxId,
        OrganizationMember.Role role
) {
}
