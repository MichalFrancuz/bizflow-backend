
package com.crudapp.fullstackbackend.service.auth;

import com.crudapp.fullstackbackend.model.AppUser;
import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.OrganizationMember;
import com.crudapp.fullstackbackend.repository.AppUserRepository;
import com.crudapp.fullstackbackend.repository.OrganizationMemberRepository;
import com.crudapp.fullstackbackend.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AppUserService appUserService;

    @Test
    void shouldRegisterUserAndCreateOrganizationWithOwnerRole() {

        when(appUserRepository.existsByEmail("user@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("SecurePassword123!"))
                .thenReturn("encoded-password");

        when(appUserRepository.save(any(AppUser.class)))
                .thenAnswer(invocation -> {
                    AppUser user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> {
                    Organization organization = invocation.getArgument(0);
                    return organization;
                });

        when(organizationMemberRepository.save(
                any(OrganizationMember.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        Long userId = appUserService.register(
                " User@Example.com ",
                "SecurePassword123!",
                " Test Organization "
        );

        assertEquals(1L, userId);

        ArgumentCaptor<AppUser> userCaptor =
                ArgumentCaptor.forClass(AppUser.class);

        verify(appUserRepository).save(userCaptor.capture());

        AppUser savedUser = userCaptor.getValue();

        assertEquals("user@example.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPasswordHash());
        assertTrue(savedUser.isEnabled());

        ArgumentCaptor<Organization> organizationCaptor =
                ArgumentCaptor.forClass(Organization.class);

        verify(organizationRepository).save(
                organizationCaptor.capture()
        );

        Organization savedOrganization = organizationCaptor.getValue();

        assertEquals("Test Organization", savedOrganization.getName());
        assertTrue(savedOrganization.isActive());

        ArgumentCaptor<OrganizationMember> memberCaptor =
                ArgumentCaptor.forClass(OrganizationMember.class);

        verify(organizationMemberRepository).save(
                memberCaptor.capture()
        );

        OrganizationMember savedMembership = memberCaptor.getValue();

        assertSame(savedUser, savedMembership.getAppUser());
        assertSame(savedOrganization, savedMembership.getOrganization());
        assertEquals(
                OrganizationMember.Role.OWNER,
                savedMembership.getRole()
        );

        verify(passwordEncoder).encode("SecurePassword123!");
    }

    @Test
    void shouldRejectDuplicateEmail() {

        when(appUserRepository.existsByEmail("user@example.com"))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> appUserService.register(
                        "user@example.com",
                        "SecurePassword123!",
                        "Test Organization"
                )
        );

        verify(appUserRepository, never()).save(any(AppUser.class));
        verify(organizationRepository, never()).save(any(Organization.class));
        verify(organizationMemberRepository, never())
                .save(any(OrganizationMember.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldRejectEmptyPassword() {

        assertThrows(
                IllegalArgumentException.class,
                () -> appUserService.register(
                        "user@example.com",
                        " ",
                        "Test Organization"
                )
        );

        verify(appUserRepository, never()).save(any(AppUser.class));
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    void shouldRejectEmptyOrganizationName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> appUserService.register(
                        "user@example.com",
                        "SecurePassword123!",
                        " "
                )
        );

        verify(appUserRepository, never()).save(any(AppUser.class));
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    void shouldFindUserByNormalizedEmail() {

        AppUser user = new AppUser();

        when(appUserRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        Optional<AppUser> result =
                appUserService.findByEmail(" User@Example.com ");

        assertTrue(result.isPresent());
        assertSame(user, result.get());

        verify(appUserRepository).findByEmail("user@example.com");
    }

    @Test
    void shouldAuthenticateUserWithValidCredentials() {

        AppUser user = new AppUser();
        user.setEmail("user@example.com");
        user.setPasswordHash("encoded-password");
        user.setEnabled(true);

        when(appUserRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "SecurePassword123!",
                "encoded-password"
        )).thenReturn(true);

        AppUser result = appUserService.authenticate(
                " User@Example.com ",
                "SecurePassword123!"
        );

        assertSame(user, result);

        verify(passwordEncoder).matches(
                "SecurePassword123!",
                "encoded-password"
        );
    }

    @Test
    void shouldRejectInvalidPassword() {

        AppUser user = new AppUser();
        user.setEmail("user@example.com");
        user.setPasswordHash("encoded-password");
        user.setEnabled(true);

        when(appUserRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword123!",
                "encoded-password"
        )).thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> appUserService.authenticate(
                        "user@example.com",
                        "WrongPassword123!"
                )
        );
    }

    @Test
    void shouldRejectNonExistingUser() {

        when(appUserRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> appUserService.authenticate(
                        "unknown@example.com",
                        "SecurePassword123!"
                )
        );

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void shouldRejectDisabledUser() {

        AppUser user = new AppUser();
        user.setEmail("user@example.com");
        user.setPasswordHash("encoded-password");
        user.setEnabled(false);

        when(appUserRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                BadCredentialsException.class,
                () -> appUserService.authenticate(
                        "user@example.com",
                        "SecurePassword123!"
                )
        );

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}
