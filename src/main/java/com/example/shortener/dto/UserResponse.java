package com.example.shortener.dto;

import com.example.shortener.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "User details")
public record UserResponse(
    @Schema(description = "User ID")
    Long id,
    @Schema(description = "User display name")
    String name,
    @Schema(description = "User email address")
    String email,
    @Schema(description = "Account creation timestamp")
    Instant createdAt,
    @Schema(description = "Last login timestamp")
    Instant lastLogin
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getLastLogin()
        );
    }
}
