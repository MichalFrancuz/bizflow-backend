
package com.crudapp.fullstackbackend.service.organization;

import com.crudapp.fullstackbackend.dto.organization.member.AddOrganizationMemberRequest;
import com.crudapp.fullstackbackend.dto.organization.member.OrganizationMemberResponse;
import com.crudapp.fullstackbackend.dto.organization.member.UpdateOrganizationMemberRoleRequest;
import com.crudapp.fullstackbackend.model.AppUser;
import com.crudapp.fullstackbackend.model.OrganizationMember;
import com.crudapp.fullstackbackend.model.OrganizationMember.Role;
import com.crudapp.fullstackbackend.repository.AppUserRepository;
import com.crudapp.fullstackbackend.repository.OrganizationMemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class OrganizationMemberService {

    private static final Set<Role> ADMIN_MANAGED_ROLES =
            Set.of(Role.MANAGER, Role.EMPLOYEE);

    private final OrganizationAccessService organizationAccessService;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final AppUserRepository appUserRepository;

    public OrganizationMemberService(
            OrganizationAccessService organizationAccessService,
            OrganizationMemberRepository organizationMemberRepository,
            AppUserRepository appUserRepository
    ) {
        this.organizationAccessService = organizationAccessService;
        this.organizationMemberRepository = organizationMemberRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<OrganizationMemberResponse> getOrganizationMembers(
            Long organizationId
    ) {
        organizationAccessService.requireMembership(organizationId);

        return organizationMemberRepository
                .findByOrganizationId(organizationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrganizationMemberResponse addMember(
            Long organizationId,
            AddOrganizationMemberRequest request
    ) {
        OrganizationMember actor =
                organizationAccessService.requireCurrentMembership(
                        organizationId
                );

        Role actorRole = actor.getRole();
        Role requestedRole = request.role();

        validateRoleAssignment(actorRole, requestedRole);

        String normalizedEmail = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        AppUser appUser = appUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "User with this email does not exist"
                        )
                );

        if (!appUser.isEnabled()) {
            throw new IllegalStateException(
                    "User account is disabled"
            );
        }

        if (organizationMemberRepository
                .existsByAppUserIdAndOrganizationId(
                        appUser.getId(),
                        organizationId
                )) {
            throw new IllegalStateException(
                    "User is already a member of this organization"
            );
        }

        OrganizationMember membership = new OrganizationMember();
        membership.setOrganization(actor.getOrganization());
        membership.setAppUser(appUser);
        membership.setRole(requestedRole);

        return toResponse(
                organizationMemberRepository.save(membership)
        );
    }

    @Transactional
    public OrganizationMemberResponse updateMemberRole(
            Long organizationId,
            Long membershipId,
            UpdateOrganizationMemberRoleRequest request
    ) {
        OrganizationMember actor =
                organizationAccessService.requireCurrentMembership(
                        organizationId
                );

        OrganizationMember target = findMembership(
                membershipId,
                organizationId
        );

        if (target.getRole() == Role.OWNER) {
            throw new AccessDeniedException(
                    "Owner role cannot be changed through this operation"
            );
        }

        // Check whether the actor can manage the target's current role.
        validateRoleAssignment(actor.getRole(), target.getRole());

        // Check whether the actor can assign the requested new role.
        validateRoleAssignment(actor.getRole(), request.role());

        target.setRole(request.role());

        return toResponse(target);
    }

    @Transactional
    public void removeMember(
            Long organizationId,
            Long membershipId
    ) {
        OrganizationMember actor =
                organizationAccessService.requireCurrentMembership(
                        organizationId
                );

        OrganizationMember target = findMembership(
                membershipId,
                organizationId
        );

        if (target.getRole() == Role.OWNER) {
            throw new AccessDeniedException(
                    "Owner cannot be removed through this operation"
            );
        }

        validateRoleAssignment(actor.getRole(), target.getRole());

        organizationMemberRepository.delete(target);
    }

    private void validateRoleAssignment(
            Role actorRole,
            Role targetRole
    ) {
        if (actorRole == Role.OWNER) {
            if (targetRole == Role.OWNER) {
                throw new AccessDeniedException(
                        "Owner role cannot be assigned through this operation"
                );
            }

            return;
        }

        if (actorRole == Role.ADMIN) {
            if (!ADMIN_MANAGED_ROLES.contains(targetRole)) {
                throw new AccessDeniedException(
                        "Administrators can only manage MANAGER and EMPLOYEE roles"
                );
            }

            return;
        }

        throw new AccessDeniedException(
                "Insufficient permissions to manage organization members"
        );
    }

    private OrganizationMember findMembership(
            Long membershipId,
            Long organizationId
    ) {
        return organizationMemberRepository
                .findByIdAndOrganizationId(
                        membershipId,
                        organizationId
                )
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Organization member not found"
                        )
                );
    }

    private OrganizationMemberResponse toResponse(
            OrganizationMember membership
    ) {
        return new OrganizationMemberResponse(
                membership.getId(),
                membership.getAppUser().getId(),
                membership.getAppUser().getEmail(),
                membership.getRole(),
                membership.getCreatedAt()
        );
    }
}
