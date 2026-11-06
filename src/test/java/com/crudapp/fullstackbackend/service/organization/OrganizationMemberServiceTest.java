
package com.crudapp.fullstackbackend.service.organization;

import com.crudapp.fullstackbackend.dto.organization.member.AddOrganizationMemberRequest;
import com.crudapp.fullstackbackend.dto.organization.member.UpdateOrganizationMemberRoleRequest;
import com.crudapp.fullstackbackend.model.AppUser;
import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.OrganizationMember;
import com.crudapp.fullstackbackend.model.OrganizationMember.Role;
import com.crudapp.fullstackbackend.repository.AppUserRepository;
import com.crudapp.fullstackbackend.repository.OrganizationMemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationMemberServiceTest {

    @Mock
    private OrganizationAccessService organizationAccessService;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private OrganizationMemberService organizationMemberService;

    @Test
    void shouldReturnOrganizationMembers() {
        OrganizationMember member = createMember(
                Role.EMPLOYEE,
                2L,
                "employee@example.com"
        );

        when(organizationMemberRepository.findByOrganizationId(10L))
                .thenReturn(List.of(member));

        var result = organizationMemberService.getOrganizationMembers(10L);

        assertEquals(1, result.size());
        assertEquals("employee@example.com", result.getFirst().email());
        assertEquals(Role.EMPLOYEE, result.getFirst().role());

        verify(organizationAccessService).requireMembership(10L);
    }

    @Test
    void shouldAddMemberSuccessfully() {
        Organization organization = new Organization();

        OrganizationMember owner = new OrganizationMember();
        owner.setOrganization(organization);
        owner.setRole(Role.OWNER);

        AppUser appUser = new AppUser();
        appUser.setId(2L);
        appUser.setEmail("employee@example.com");
        appUser.setEnabled(true);

        when(organizationAccessService.requireCurrentMembership(10L))
                .thenReturn(owner);

        when(appUserRepository.findByEmail("employee@example.com"))
                .thenReturn(Optional.of(appUser));

        when(organizationMemberRepository
                .existsByAppUserIdAndOrganizationId(2L, 10L))
                .thenReturn(false);

        when(organizationMemberRepository.save(any(OrganizationMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = organizationMemberService.addMember(
                10L,
                new AddOrganizationMemberRequest(
                        " EMPLOYEE@EXAMPLE.COM ",
                        Role.EMPLOYEE
                )
        );

        assertEquals(2L, result.userId());
        assertEquals("employee@example.com", result.email());
        assertEquals(Role.EMPLOYEE, result.role());

        verify(organizationMemberRepository).save(any(OrganizationMember.class));
    }

    @Test
    void shouldRejectAddingAdminByAnotherAdmin() {
        OrganizationMember admin = new OrganizationMember();
        admin.setRole(Role.ADMIN);

        when(organizationAccessService.requireCurrentMembership(10L))
                .thenReturn(admin);

        assertThrows(
                AccessDeniedException.class,
                () -> organizationMemberService.addMember(
                        10L,
                        new AddOrganizationMemberRequest(
                                "admin@example.com",
                                Role.ADMIN
                        )
                )
        );

        verifyNoInteractions(appUserRepository);
        verify(organizationMemberRepository, never())
                .save(any(OrganizationMember.class));
    }

    @Test
    void shouldRejectAddingAlreadyExistingMember() {
        OrganizationMember owner = new OrganizationMember();
        owner.setRole(Role.OWNER);

        AppUser appUser = new AppUser();
        appUser.setId(2L);
        appUser.setEmail("employee@example.com");
        appUser.setEnabled(true);

        when(organizationAccessService.requireCurrentMembership(10L))
                .thenReturn(owner);

        when(appUserRepository.findByEmail("employee@example.com"))
                .thenReturn(Optional.of(appUser));

        when(organizationMemberRepository
                .existsByAppUserIdAndOrganizationId(2L, 10L))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> organizationMemberService.addMember(
                        10L,
                        new AddOrganizationMemberRequest(
                                "employee@example.com",
                                Role.EMPLOYEE
                        )
                )
        );

        verify(organizationMemberRepository, never())
                .save(any(OrganizationMember.class));
    }

    @Test
    void shouldAllowAdminToPromoteEmployeeToManager() {
        OrganizationMember admin = new OrganizationMember();
        admin.setRole(Role.ADMIN);

        OrganizationMember target = createMember(
                Role.EMPLOYEE,
                2L,
                "employee@example.com"
        );

        when(organizationAccessService.requireCurrentMembership(10L))
                .thenReturn(admin);

        when(organizationMemberRepository.findByIdAndOrganizationId(100L, 10L))
                .thenReturn(Optional.of(target));

        var result = organizationMemberService.updateMemberRole(
                10L,
                100L,
                new UpdateOrganizationMemberRoleRequest(Role.MANAGER)
        );

        assertEquals(Role.MANAGER, target.getRole());
        assertEquals(Role.MANAGER, result.role());
    }

    @Test
    void shouldRejectAdminChangingAnotherAdminRole() {
        OrganizationMember admin = new OrganizationMember();
        admin.setRole(Role.ADMIN);

        OrganizationMember target = createMember(
                Role.ADMIN,
                2L,
                "another-admin@example.com"
        );

        when(organizationAccessService.requireCurrentMembership(10L))
                .thenReturn(admin);

        when(organizationMemberRepository.findByIdAndOrganizationId(100L, 10L))
                .thenReturn(Optional.of(target));

        assertThrows(
                AccessDeniedException.class,
                () -> organizationMemberService.updateMemberRole(
                        10L,
                        100L,
                        new UpdateOrganizationMemberRoleRequest(Role.EMPLOYEE)
                )
        );

        assertEquals(Role.ADMIN, target.getRole());
    }

    @Test
    void shouldRejectChangingOwnerRole() {
        OrganizationMember owner = new OrganizationMember();
        owner.setRole(Role.OWNER);

        OrganizationMember target = createMember(
                Role.OWNER,
                2L,
                "owner@example.com"
        );

        when(organizationAccessService.requireCurrentMembership(10L))
                .thenReturn(owner);

        when(organizationMemberRepository.findByIdAndOrganizationId(100L, 10L))
                .thenReturn(Optional.of(target));

        assertThrows(
                AccessDeniedException.class,
                () -> organizationMemberService.updateMemberRole(
                        10L,
                        100L,
                        new UpdateOrganizationMemberRoleRequest(Role.ADMIN)
                )
        );

        assertEquals(Role.OWNER, target.getRole());
    }

    @Test
    void shouldRejectRemovingOwner() {
        OrganizationMember owner = new OrganizationMember();
        owner.setRole(Role.OWNER);

        OrganizationMember target = createMember(
                Role.OWNER,
                2L,
                "owner@example.com"
        );

        when(organizationAccessService.requireCurrentMembership(10L))
                .thenReturn(owner);

        when(organizationMemberRepository.findByIdAndOrganizationId(100L, 10L))
                .thenReturn(Optional.of(target));

        assertThrows(
                AccessDeniedException.class,
                () -> organizationMemberService.removeMember(10L, 100L)
        );

        verify(organizationMemberRepository, never())
                .delete(any(OrganizationMember.class));
    }

    @Test
    void shouldRejectRemovingMemberFromDifferentOrganization() {
        OrganizationMember owner = new OrganizationMember();
        owner.setRole(Role.OWNER);

        when(organizationAccessService.requireCurrentMembership(10L))
                .thenReturn(owner);

        when(organizationMemberRepository.findByIdAndOrganizationId(100L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> organizationMemberService.removeMember(10L, 100L)
        );

        verify(organizationMemberRepository, never())
                .delete(any(OrganizationMember.class));
    }

    private OrganizationMember createMember(
            Role role,
            Long userId,
            String email
    ) {
        AppUser appUser = new AppUser();
        appUser.setId(userId);
        appUser.setEmail(email);

        OrganizationMember membership = new OrganizationMember();
        membership.setAppUser(appUser);
        membership.setRole(role);

        return membership;
    }
}
