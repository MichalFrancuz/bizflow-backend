
package com.crudapp.fullstackbackend.service.organization;

import com.crudapp.fullstackbackend.model.AppUser;
import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.OrganizationMember;
import com.crudapp.fullstackbackend.repository.AppUserRepository;
import com.crudapp.fullstackbackend.repository.OrganizationMemberRepository;
import com.crudapp.fullstackbackend.repository.OrganizationRepository;
import com.crudapp.fullstackbackend.service.auth.CurrentUserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private OrganizationService organizationService;

    @Test
    void shouldCreateOrganizationAndAssignOwnerRole() {
        AppUser owner = new AppUser();
        owner.setId(1L);
        owner.setEnabled(true);

        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(appUserRepository.findById(1L))
                .thenReturn(Optional.of(owner));

        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> {
                    Organization organization = invocation.getArgument(0);

                    try {
                        var idField = Organization.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(organization, 10L);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }

                    return organization;
                });

        Long organizationId = organizationService.createOrganization(
                "  BizFlow Ltd  ",
                " 1234567890 "
        );

        assertEquals(10L, organizationId);

        ArgumentCaptor<Organization> organizationCaptor =
                ArgumentCaptor.forClass(Organization.class);

        verify(organizationRepository).save(organizationCaptor.capture());

        Organization savedOrganization = organizationCaptor.getValue();

        assertEquals("BizFlow Ltd", savedOrganization.getName());
        assertEquals("1234567890", savedOrganization.getTaxId());

        ArgumentCaptor<OrganizationMember> memberCaptor =
                ArgumentCaptor.forClass(OrganizationMember.class);

        verify(organizationMemberRepository).save(memberCaptor.capture());

        OrganizationMember membership = memberCaptor.getValue();

        assertSame(owner, membership.getAppUser());
        assertSame(savedOrganization, membership.getOrganization());
        assertEquals(OrganizationMember.Role.OWNER, membership.getRole());
    }

    @Test
    void shouldRejectEmptyOrganizationName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> organizationService.createOrganization(" ", null)
        );

        verifyNoInteractions(
                currentUserService,
                appUserRepository,
                organizationRepository,
                organizationMemberRepository
        );
    }

    @Test
    void shouldRejectNonExistingOwner() {
        when(currentUserService.getCurrentUserId()).thenReturn(999L);
        when(appUserRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> organizationService.createOrganization("BizFlow", null)
        );

        verify(organizationRepository, never()).save(any(Organization.class));
        verify(organizationMemberRepository, never()).save(any(OrganizationMember.class));
    }

    @Test
    void shouldRejectDisabledOwnerAccount() {
        AppUser owner = new AppUser();
        owner.setId(1L);
        owner.setEnabled(false);

        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(appUserRepository.findById(1L))
                .thenReturn(Optional.of(owner));

        assertThrows(
                IllegalStateException.class,
                () -> organizationService.createOrganization("BizFlow", null)
        );

        verify(organizationRepository, never()).save(any(Organization.class));
        verify(organizationMemberRepository, never()).save(any(OrganizationMember.class));
    }
}
