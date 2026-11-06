
package com.crudapp.fullstackbackend.config;

import com.crudapp.fullstackbackend.model.AppUser;
import com.crudapp.fullstackbackend.repository.AppUserRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class JwtConfig {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecretKey jwtSecretKey() {

        byte[] decodedSecret = Base64.getDecoder().decode(jwtSecret);

        if (decodedSecret.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must contain at least 256 bits"
            );
        }

        return new SecretKeySpec(decodedSecret, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(
                new ImmutableSecret<>(jwtSecretKey)
        );
    }

    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            AppUserRepository appUserRepository
    ) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer("bizflow-api");

        OAuth2TokenValidator<Jwt> accountStatusValidator = jwt -> {

            String subject = jwt.getSubject();

            if (subject == null || subject.isBlank()) {
                return invalidToken("Missing user ID in JWT subject");
            }

            Long userId;

            try {
                userId = Long.valueOf(subject);
            } catch (NumberFormatException exception) {
                return invalidToken("Invalid user ID in JWT subject");
            }

            boolean accountEnabled = appUserRepository.findById(userId)
                    .map(AppUser::isEnabled)
                    .orElse(false);

            if (!accountEnabled) {
                return invalidToken(
                        "User account does not exist or is disabled"
                );
            }

            return OAuth2TokenValidatorResult.success();
        };

        OAuth2TokenValidator<Jwt> combinedValidator =
                new DelegatingOAuth2TokenValidator<>(
                        issuerValidator,
                        accountStatusValidator
                );

        decoder.setJwtValidator(combinedValidator);

        return decoder;
    }

    private OAuth2TokenValidatorResult invalidToken(String description) {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error(
                        "invalid_token",
                        description,
                        null
                )
        );
    }
}
