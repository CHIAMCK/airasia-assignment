package com.example.shortener.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "url_mapping")
public class UrlMapping {

    @Id
    @Column(name = "short_code", length = 16)
    private String shortCode;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    protected UrlMapping() {}

    public UrlMapping(String shortCode, String originalUrl) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = Instant.now();
    }

    public UrlMapping(String shortCode, String originalUrl, User user) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.user = user;
        this.createdAt = Instant.now();
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public User getUser() {
        return user;
    }
}
