
package com.crudapp.fullstackbackend.service.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public Long getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new IllegalStateException(
                    "Authenticated user is not represented by a JWT"
            );
        }

        String subject = jwtAuthentication.getToken().getSubject();

        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Invalid user ID in JWT subject",
                    exception
            );
        }
    }
}
