
package com.crudapp.fullstackbackend.config;

import com.crudapp.fullstackbackend.model.AppUser;
import com.crudapp.fullstackbackend.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("bizflow-test")
@TestPropertySource(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {

        mockMvc.perform(get("/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowRequestWithValidJwt() throws Exception {

        AppUser activeUser = new AppUser();
        activeUser.setId(15L);
        activeUser.setEmail("user@example.com");
        activeUser.setPasswordHash("test-password");
        activeUser.setEnabled(true);

        when(appUserRepository.findById(15L))
                .thenReturn(Optional.of(activeUser));

        String token = generateValidToken();

        mockMvc.perform(get("/user")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectRequestWhenUserAccountIsDisabled() throws Exception {

        AppUser disabledUser = new AppUser();
        disabledUser.setId(15L);
        disabledUser.setEmail("user@example.com");
        disabledUser.setPasswordHash("test-password");
        disabledUser.setEnabled(false);

        when(appUserRepository.findById(15L))
                .thenReturn(Optional.of(disabledUser));

        String token = generateValidToken();

        mockMvc.perform(get("/user")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRequestWhenUserAccountDoesNotExist() throws Exception {

        when(appUserRepository.findById(15L))
                .thenReturn(Optional.empty());

        String token = generateValidToken();

        mockMvc.perform(get("/user")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRequestWithInvalidJwt() throws Exception {

        mockMvc.perform(get("/user")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer invalid.token.value"
                        ))
                .andExpect(status().isUnauthorized());
    }

    private String generateValidToken() {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("bizflow-api")
                .subject("15")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .claim("email", "user@example.com")
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }
}
