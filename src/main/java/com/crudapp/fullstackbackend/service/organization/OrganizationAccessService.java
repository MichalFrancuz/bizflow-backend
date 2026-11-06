
package com.crudapp.fullstackbackend.service.organization;

import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.OrganizationMember;
import com.crudapp.fullstackbackend.model.OrganizationMember.Role;
import com.crudapp.fullstackbackend.repository.OrganizationMemberRepository;
import com.crudapp.fullstackbackend.repository.OrganizationRepository;
import com.crudapp.fullstackbackend.service.auth.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class OrganizationAccessService {

    private final CurrentUserService currentUserService;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationRepository organizationRepository;

    public OrganizationAccessService(
            CurrentUserService currentUserService,
            OrganizationMemberRepository organizationMemberRepository,
            OrganizationRepository organizationRepository
    ) {
        this.currentUserService = currentUserService;
        this.organizationMemberRepository = organizationMemberRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional(readOnly = true)
    public OrganizationMember requireCurrentMembership(
            Long organizationId
    ) {
        Long userId = currentUserService.getCurrentUserId();

        OrganizationMember membership = organizationMemberRepository
                .findByAppUserIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new AccessDeniedException(
                        "You do not have access to this organization"
                ));

        Organization organization = organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Organization is not available"
                ));

        if (!organization.isActive()) {
            throw new AccessDeniedException(
                    "Organization is inactive"
            );
        }

        return membership;
    }

    @Transactional(readOnly = true)
    public Organization requireMembership(Long organizationId) {
        return requireCurrentMembership(organizationId)
                .getOrganization();
    }

    @Transactional(readOnly = true)
    public Organization requireRole(
            Long organizationId,
            Set<Role> allowedRoles
    ) {
        OrganizationMember membership =
                requireCurrentMembership(organizationId);

        if (!allowedRoles.contains(membership.getRole())) {
            throw new AccessDeniedException(
                    "Insufficient permissions for this operation"
            );
        }

        return membership.getOrganization();
    }
}
