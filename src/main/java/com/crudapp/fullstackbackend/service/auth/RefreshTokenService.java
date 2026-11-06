
package com.crudapp.fullstackbackend.service.auth;

import com.crudapp.fullstackbackend.model.AppUser;
import com.crudapp.fullstackbackend.model.RefreshToken;
import com.crudapp.fullstackbackend.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpiration;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-token-expiration}")
            long refreshTokenExpiration
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @Transactional
    public String createRefreshToken(AppUser appUser) {

        if (!appUser.isEnabled()) {
            throw new IllegalStateException("User account is disabled");
        }

        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setAppUser(appUser);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setFamilyId(UUID.randomUUID().toString());
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(
                LocalDateTime.now().plusSeconds(refreshTokenExpiration)
        );

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public RotationResult rotateRefreshToken(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }

        String tokenHash = hashToken(rawToken);

        RefreshToken currentToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElse(null);

        if (currentToken == null) {
            return null;
        }

        // Reuse of a revoked token invalidates the entire token family.
        if (currentToken.getRevokedAt() != null) {
            revokeFamily(currentToken.getFamilyId());
            return null;
        }

        // Expired tokens cannot be used.
        if (!currentToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            currentToken.setRevokedAt(LocalDateTime.now());
            return null;
        }

        AppUser appUser = currentToken.getAppUser();

        if (!appUser.isEnabled()) {
            revokeFamily(currentToken.getFamilyId());
            return null;
        }

        String newRawToken = generateSecureToken();
        String newTokenHash = hashToken(newRawToken);

        RefreshToken newToken = new RefreshToken();

        newToken.setAppUser(appUser);
        newToken.setTokenHash(newTokenHash);
        newToken.setFamilyId(currentToken.getFamilyId());
        newToken.setCreatedAt(LocalDateTime.now());
        newToken.setExpiresAt(
                LocalDateTime.now().plusSeconds(refreshTokenExpiration)
        );

        currentToken.setRevokedAt(LocalDateTime.now());
        currentToken.setReplacedByTokenHash(newTokenHash);

        refreshTokenRepository.save(newToken);

        return new RotationResult(appUser, newRawToken);
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        String tokenHash = hashToken(rawToken);

        refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .ifPresent(token -> revokeFamily(token.getFamilyId()));
    }

    private void revokeFamily(String familyId) {

        LocalDateTime now = LocalDateTime.now();

        refreshTokenRepository.findAllByFamilyId(familyId)
                .stream()
                .filter(token -> token.getRevokedAt() == null)
                .forEach(token -> token.setRevokedAt(now));
    }

    private String generateSecureToken() {

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }

    public record RotationResult(
            AppUser appUser,
            String refreshToken
    ) {
    }
}
