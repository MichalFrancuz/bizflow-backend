package com.crudapp.fullstackbackend.dto.auth;

public record LoginResponse(
        Long userId,
        String email,
        String tokenType,
        String accessToken,
        long expiresIn
) {
}