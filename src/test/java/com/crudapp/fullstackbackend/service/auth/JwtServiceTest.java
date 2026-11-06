
package com.crudapp.fullstackbackend.service.auth;

import com.crudapp.fullstackbackend.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(jwtEncoder, 900);
    }

    @Test
    void shouldGenerateAccessTokenWithCorrectClaims() {

        AppUser user = new AppUser();
        user.setId(15L);
        user.setEmail("user@example.com");

        Jwt jwt = mock(Jwt.class);

        when(jwt.getTokenValue()).thenReturn("test-access-token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(jwt);

        String token = jwtService.generateAccessToken(user);

        assertEquals("test-access-token", token);

        ArgumentCaptor<JwtEncoderParameters> captor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        verify(jwtEncoder).encode(captor.capture());

        var claims = captor.getValue().getClaims();

        assertEquals("bizflow-api", claims.getClaimAsString("iss"));
        assertEquals("15", claims.getSubject());
        assertEquals("user@example.com", claims.getClaim("email"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiresAt());

        assertEquals(
                Duration.ofSeconds(900),
                Duration.between(
                        claims.getIssuedAt(),
                        claims.getExpiresAt()
                )
        );
    }

    @Test
    void shouldReturnConfiguredTokenExpiration() {

        assertEquals(900, jwtService.getAccessTokenExpiration());
    }
}
