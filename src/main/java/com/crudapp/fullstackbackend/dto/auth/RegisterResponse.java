package com.crudapp.fullstackbackend.dto.auth;

public record RegisterResponse(
        Long userId,
        String email,
        String message
) {
}