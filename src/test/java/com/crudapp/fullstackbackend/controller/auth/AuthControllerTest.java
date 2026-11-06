
package com.crudapp.fullstackbackend.controller.auth;

import com.crudapp.fullstackbackend.model.AppUser;
import com.crudapp.fullstackbackend.service.auth.AppUserService;
import com.crudapp.fullstackbackend.service.auth.JwtService;
import com.crudapp.fullstackbackend.service.auth.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {

        when(appUserService.register(
                "user@example.com",
                "MyStrongPassword123",
                "Test Organization"
        )).thenReturn(15L);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "user@example.com",
                                    "password": "MyStrongPassword123",
                                    "organizationName": "Test Organization"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(15))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.message").value("Registration successful"));

        verify(appUserService).register(
                "user@example.com",
                "MyStrongPassword123",
                "Test Organization"
        );
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "invalid-email",
                                    "password": "MyStrongPassword123",
                                    "organizationName": "Test Organization"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verifyNoInteractions(appUserService);
    }

    @Test
    void shouldRejectShortPassword() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "user@example.com",
                                    "password": "short",
                                    "organizationName": "Test Organization"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        verifyNoInteractions(appUserService);
    }

    @Test
    void shouldRejectMissingOrganizationName() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "user@example.com",
                                    "password": "MyStrongPassword123",
                                    "organizationName": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.organizationName").exists());

        verifyNoInteractions(appUserService);
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {

        when(appUserService.register(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException(
                        "Email is already registered"
                ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "existing@example.com",
                                    "password": "MyStrongPassword123",
                                    "organizationName": "Existing Organization"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void shouldLoginUserSuccessfully() throws Exception {

        AppUser user = new AppUser();
        user.setId(15L);
        user.setEmail("user@example.com");
        user.setEnabled(true);

        when(appUserService.authenticate(
                "user@example.com",
                "SecurePassword123!"
        )).thenReturn(user);

        when(jwtService.generateAccessToken(user))
                .thenReturn("test-access-token");

        when(jwtService.getAccessTokenExpiration())
                .thenReturn(900L);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "user@example.com",
                                    "password": "SecurePassword123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(15))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.expiresIn").value(900));

        verify(appUserService).authenticate(
                "user@example.com",
                "SecurePassword123!"
        );

        verify(jwtService).generateAccessToken(user);
    }

    @Test
    void shouldRejectInvalidLoginCredentials() throws Exception {

        when(appUserService.authenticate(
                "user@example.com",
                "WrongPassword123!"
        )).thenThrow(new BadCredentialsException(
                "Invalid email or password"
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "user@example.com",
                                    "password": "WrongPassword123!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication failed"));

        verify(jwtService, never()).generateAccessToken(any(AppUser.class));
    }

    @Test
    void shouldRejectInvalidEmailDuringLogin() throws Exception {

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "invalid-email",
                                    "password": "SecurePassword123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(appUserService, never()).authenticate(anyString(), anyString());
    }
}
