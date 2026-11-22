package edu.kuet.classsync.auth.dto;

import edu.kuet.classsync.user.dto.UserResponse;

/** Login result: a Bearer JWT, its lifetime in seconds and the logged-in user. */
public record AuthResponse(String token, String tokenType, long expiresIn, UserResponse user) {

    public static AuthResponse bearer(String token, long expiresIn, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresIn, user);
    }
}
