package com.example.shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to shorten a URL")
public record ShortenRequest(
    @Schema(description = "The long URL to shorten (http, https, or ftp)", example = "https://example.com/long-url", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$", message = "Invalid URL format")
    String url,
    @Schema(description = "Optional user ID for authenticated shortening (omit for anonymous, which is rate-limited)", nullable = true)
    Long userId
) {}
