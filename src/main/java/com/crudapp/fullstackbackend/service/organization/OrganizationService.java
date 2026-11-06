
package com.crudapp.fullstackbackend.service.organization;

import com.crudapp.fullstackbackend.model.AppUser;
import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.OrganizationMember;
import com.crudapp.fullstackbackend.repository.AppUserRepository;
import com.crudapp.fullstackbackend.repository.OrganizationMemberRepository;
import com.crudapp.fullstackbackend.repository.OrganizationRepository;
import com.crudapp.fullstackbackend.service.auth.CurrentUserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crudapp.fullstackbackend.dto.organization.OrganizationResponse;
import java.util.List;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            AppUserRepository appUserRepository,
            CurrentUserService currentUserService
    ) {
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.appUserRepository = appUserRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public Long createOrganization(String name, String taxId) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Organization name cannot be empty"
            );
        }

        Long ownerUserId = currentUserService.getCurrentUserId();

        AppUser owner = appUserRepository.findById(ownerUserId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Owner user not found")
                );

        if (!owner.isEnabled()) {
            throw new IllegalStateException("Owner account is disabled");
        }

        Organization organization = new Organization();
        organization.setName(name.trim());
        organization.setTaxId(
                taxId == null || taxId.isBlank()
                        ? null
                        : taxId.trim()
        );

        Organization savedOrganization =
                organizationRepository.save(organization);

        OrganizationMember membership = new OrganizationMember();
        membership.setOrganization(savedOrganization);
        membership.setAppUser(owner);
        membership.setRole(OrganizationMember.Role.OWNER);

        organizationMemberRepository.save(membership);

        return savedOrganization.getId();
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> getCurrentUserOrganizations() {

        Long userId = currentUserService.getCurrentUserId();

        return organizationMemberRepository.findByAppUserId(userId)
                .stream()
                .filter(membership -> membership.getOrganization().isActive())
                .map(membership -> new OrganizationResponse(
                        membership.getOrganization().getId(),
                        membership.getOrganization().getName(),
                        membership.getOrganization().getTaxId(),
                        membership.getRole()
                ))
                .toList();
    }
}
