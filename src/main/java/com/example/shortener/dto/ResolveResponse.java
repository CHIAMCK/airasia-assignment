package com.example.shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resolved URL (for API lookup; use /r/{shortCode} for browser redirect)")
public record ResolveResponse(
        @Schema(description = "The original long URL", example = "https://example.com/page")
        String originalUrl
) {}
