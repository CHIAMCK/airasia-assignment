package com.example.shortener.controller;

import com.example.shortener.dto.ShortenRequest;
import com.example.shortener.dto.ShortenResponse;
import com.example.shortener.service.RateLimitService;
import com.example.shortener.service.ShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@Tag(name = "URL Shortener", description = "Shorten URLs and resolve redirects")
public class ShortenerController {

    private final ShortenerService shortenerService;
    private final RateLimitService rateLimitService;

    public ShortenerController(ShortenerService shortenerService, RateLimitService rateLimitService) {
        this.shortenerService = shortenerService;
        this.rateLimitService = rateLimitService;
    }

    @Operation(summary = "Shorten a URL", description = "Creates a short URL for the given long URL. Anonymous requests (no userId) are rate-limited.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = ShortenRequest.class),
                            examples = {@ExampleObject(name = "Anonymous", value = "{\"url\": \"https://example.com/long-url\"}")})))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "URL shortened successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid URL format", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded (anonymous requests only)")
    })
    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request, HttpServletRequest httpRequest) {
        if (request.userId() == null) {
            rateLimitService.consumeAnonymousShorten();
        }
        String shortUrl = shortenerService.shorten(request.url(), request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ShortenResponse(shortUrl));
    }

    @Operation(summary = "Redirect to original URL",
            description = "Resolves the short code and redirects (302) to the original URL. " +
                    "Use by opening the URL in a browser tab or following a link—do not use Swagger UI \"Try it out\" " +
                    "(it fails due to CORS when redirecting to external URLs).")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to original URL"),
            @ApiResponse(responseCode = "404", description = "Short code not found")
    })
    @GetMapping("/r/{shortCode}")
    public ResponseEntity<Void> redirect(
            @Parameter(description = "The short code from the shortened URL") @PathVariable @NonNull String shortCode) {
        String longUrl = shortenerService.resolve(shortCode).orElse(null);
        if (longUrl != null && !longUrl.isEmpty()) {
            URI uri = URI.create(longUrl);
            if (uri != null) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(uri)
                        .build();
            }
        }

        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidUrl(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
