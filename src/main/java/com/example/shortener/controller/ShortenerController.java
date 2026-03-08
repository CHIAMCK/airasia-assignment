package com.example.shortener.controller;

import com.example.shortener.dto.ShortenRequest;
import com.example.shortener.dto.ShortenResponse;
import com.example.shortener.service.RateLimitService;
import com.example.shortener.service.ShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class ShortenerController {

    private final ShortenerService shortenerService;
    private final RateLimitService rateLimitService;

    public ShortenerController(ShortenerService shortenerService, RateLimitService rateLimitService) {
        this.shortenerService = shortenerService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request, HttpServletRequest httpRequest) {
        if (request.userId() == null) {
            rateLimitService.consumeAnonymousShorten();
        }
        String shortUrl = shortenerService.shorten(request.url(), request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ShortenResponse(shortUrl));
    }

    @GetMapping("/r/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable @NonNull String shortCode) {
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
