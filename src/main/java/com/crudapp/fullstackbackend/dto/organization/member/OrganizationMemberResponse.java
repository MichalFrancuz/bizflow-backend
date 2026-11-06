
package com.crudapp.fullstackbackend.dto.organization.member;

import com.crudapp.fullstackbackend.model.OrganizationMember.Role;

import java.time.LocalDateTime;

public record OrganizationMemberResponse(

        Long id,
        Long userId,
        String email,
        Role role,
        LocalDateTime createdAt

) {
}
