package com.example.shortener.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
    String name,
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email
) {}
