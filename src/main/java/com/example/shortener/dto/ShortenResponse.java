package com.example.shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Shortened URL response")
public record ShortenResponse(
    @Schema(description = "The full short URL (e.g. http://localhost:8080/r/abc123)", example = "http://localhost:8080/r/abc123")
    String shortUrl
) {}
