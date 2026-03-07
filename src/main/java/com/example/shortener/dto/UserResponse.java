package com.example.shortener.dto;

import com.example.shortener.entity.User;

import java.time.Instant;

public record UserResponse(Long id, String name, String email, Instant createdAt, Instant lastLogin) {

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
