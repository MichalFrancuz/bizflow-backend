
package com.crudapp.fullstackbackend.service.organization;

import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.OrganizationMember;
import com.crudapp.fullstackbackend.model.OrganizationMember.Role;
import com.crudapp.fullstackbackend.repository.OrganizationMemberRepository;
import com.crudapp.fullstackbackend.repository.OrganizationRepository;
import com.crudapp.fullstackbackend.service.auth.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationAccessServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private OrganizationAccessService organizationAccessService;

    private Organization organization;
    private OrganizationMember membership;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setName("BizFlow Ltd");
        organization.setActive(true);

        membership = new OrganizationMember();
        membership.setOrganization(organization);
        membership.setRole(Role.OWNER);

        when(currentUserService.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    void shouldAllowMemberAccessToActiveOrganization() {
        when(organizationMemberRepository
                .findByAppUserIdAndOrganizationId(1L, 10L))
                .thenReturn(Optional.of(membership));

        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(organization));

        Organization result =
                organizationAccessService.requireMembership(10L);

        assertSame(organization, result);
    }

    @Test
    void shouldRejectUserWhoIsNotOrganizationMember() {
        when(organizationMemberRepository
                .findByAppUserIdAndOrganizationId(1L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> organizationAccessService.requireMembership(10L)
        );

        verify(organizationRepository, never()).findById(anyLong());
    }

    @Test
    void shouldRejectAccessToInactiveOrganization() {
        organization.setActive(false);

        when(organizationMemberRepository
                .findByAppUserIdAndOrganizationId(1L, 10L))
                .thenReturn(Optional.of(membership));

        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(organization));

        assertThrows(
                AccessDeniedException.class,
                () -> organizationAccessService.requireMembership(10L)
        );
    }

    @Test
    void shouldRejectMemberWithoutRequiredRole() {
        membership.setRole(Role.EMPLOYEE);

        when(organizationMemberRepository
                .findByAppUserIdAndOrganizationId(1L, 10L))
                .thenReturn(Optional.of(membership));

        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(organization));

        assertThrows(
                AccessDeniedException.class,
                () -> organizationAccessService.requireRole(
                        10L,
                        Set.of(Role.OWNER, Role.ADMIN)
                )
        );
    }

    @Test
    void shouldAllowMemberWithRequiredRole() {
        membership.setRole(Role.ADMIN);

        when(organizationMemberRepository
                .findByAppUserIdAndOrganizationId(1L, 10L))
                .thenReturn(Optional.of(membership));

        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(organization));

        Organization result = organizationAccessService.requireRole(
                10L,
                Set.of(Role.OWNER, Role.ADMIN)
        );

        assertSame(organization, result);
    }
}
