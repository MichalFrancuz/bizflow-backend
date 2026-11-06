
package com.crudapp.fullstackbackend.service.auth;

import com.crudapp.fullstackbackend.model.AppUser;
import com.crudapp.fullstackbackend.model.Organization;
import com.crudapp.fullstackbackend.model.OrganizationMember;
import com.crudapp.fullstackbackend.repository.AppUserRepository;
import com.crudapp.fullstackbackend.repository.OrganizationMemberRepository;
import com.crudapp.fullstackbackend.repository.OrganizationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(
            AppUserRepository appUserRepository,
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Long register(
            String email,
            String rawPassword,
            String organizationName
    ) {

        String normalizedEmail = normalizeEmail(email);

        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        if (organizationName == null || organizationName.isBlank()) {
            throw new IllegalArgumentException(
                    "Organization name cannot be empty"
            );
        }

        String normalizedOrganizationName = organizationName.trim();

        if (normalizedOrganizationName.length() > 200) {
            throw new IllegalArgumentException(
                    "Organization name cannot exceed 200 characters"
            );
        }

        if (appUserRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("Email is already registered");
        }

        // 1. Create user account.
        AppUser appUser = new AppUser();
        appUser.setEmail(normalizedEmail);
        appUser.setPasswordHash(passwordEncoder.encode(rawPassword));
        appUser.setEnabled(true);

        AppUser savedUser = appUserRepository.save(appUser);

        // 2. Create organization.
        Organization organization = new Organization();
        organization.setName(normalizedOrganizationName);
        organization.setActive(true);

        Organization savedOrganization =
                organizationRepository.save(organization);

        // 3. Assign the user as organization owner.
        OrganizationMember membership = new OrganizationMember();
        membership.setAppUser(savedUser);
        membership.setOrganization(savedOrganization);
        membership.setRole(OrganizationMember.Role.OWNER);

        organizationMemberRepository.save(membership);

        return savedUser.getId();
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> findByEmail(String email) {
        return appUserRepository.findByEmail(normalizeEmail(email));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public AppUser authenticate(String email, String rawPassword) {

        AppUser appUser = findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException(
                        "Invalid email or password"
                ));

        if (!appUser.isEnabled()
                || !passwordEncoder.matches(
                rawPassword,
                appUser.getPasswordHash()
        )) {

            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        return appUser;
    }
}
