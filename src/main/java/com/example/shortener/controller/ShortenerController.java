package com.example.shortener.controller;

import com.example.shortener.dto.ShortenRequest;
import com.example.shortener.dto.ShortenResponse;
import com.example.shortener.service.ShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
public class ShortenerController {

    private final ShortenerService shortenerService;

    public ShortenerController(ShortenerService shortenerService) {
        this.shortenerService = shortenerService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request, HttpServletRequest httpRequest) {
        String baseUrl = httpRequest.getRequestURL().toString().replaceAll("/shorten$", "").replaceAll("/$", "");
        String shortUrl = shortenerService.shorten(request.url(), baseUrl, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ShortenResponse(shortUrl));
    }

    @GetMapping("/r/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        Optional<String> longUrl = shortenerService.resolve(shortCode);
        if (longUrl.isPresent()) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(longUrl.get())).build();
        }
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidUrl(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
