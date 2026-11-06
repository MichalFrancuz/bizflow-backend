
package com.crudapp.fullstackbackend.controller.auth;

import com.crudapp.fullstackbackend.dto.auth.LoginRequest;
import com.crudapp.fullstackbackend.dto.auth.LoginResponse;
import com.crudapp.fullstackbackend.dto.auth.RegisterRequest;
import com.crudapp.fullstackbackend.dto.auth.RegisterResponse;
import com.crudapp.fullstackbackend.model.AppUser;
import com.crudapp.fullstackbackend.service.auth.AppUserService;
import com.crudapp.fullstackbackend.service.auth.JwtService;
import com.crudapp.fullstackbackend.service.auth.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "BIZFLOW_REFRESH";

    private final AppUserService appUserService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final long refreshTokenExpiration;
    private final boolean refreshCookieSecure;

    public AuthController(
            AppUserService appUserService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            @Value("${app.jwt.refresh-token-expiration}")
            long refreshTokenExpiration,
            @Value("${app.jwt.refresh-cookie-secure:false}")
            boolean refreshCookieSecure
    ) {
        this.appUserService = appUserService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.refreshCookieSecure = refreshCookieSecure;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        Long userId = appUserService.register(
                request.email(),
                request.password(),
                request.organizationName()
        );

        RegisterResponse response = new RegisterResponse(
                userId,
                request.email().trim().toLowerCase(),
                "Registration successful"
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        AppUser appUser = appUserService.authenticate(
                request.email(),
                request.password()
        );

        String accessToken = jwtService.generateAccessToken(appUser);
        String refreshToken =
                refreshTokenService.createRefreshToken(appUser);

        LoginResponse response = new LoginResponse(
                appUser.getId(),
                appUser.getEmail(),
                "Bearer",
                accessToken,
                jwtService.getAccessTokenExpiration()
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        createRefreshCookie(refreshToken).toString()
                )
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(
                    name = REFRESH_COOKIE_NAME,
                    required = false
            ) String refreshToken
    ) {

        RefreshTokenService.RotationResult result =
                refreshTokenService.rotateRefreshToken(refreshToken);

        if (result == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(
                            HttpHeaders.SET_COOKIE,
                            clearRefreshCookie().toString()
                    )
                    .build();
        }

        AppUser appUser = result.appUser();

        String accessToken = jwtService.generateAccessToken(appUser);

        LoginResponse response = new LoginResponse(
                appUser.getId(),
                appUser.getEmail(),
                "Bearer",
                accessToken,
                jwtService.getAccessTokenExpiration()
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        createRefreshCookie(result.refreshToken()).toString()
                )
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(
                    name = REFRESH_COOKIE_NAME,
                    required = false
            ) String refreshToken
    ) {

        refreshTokenService.revokeRefreshToken(refreshToken);

        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        clearRefreshCookie().toString()
                )
                .build();
    }

    private ResponseCookie createRefreshCookie(String refreshToken) {

        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(refreshTokenExpiration))
                .build();
    }

    private ResponseCookie clearRefreshCookie() {

        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();

        return ResponseEntity.noContent().build();
    }
}
