package com.example.shortener.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "keys")
public class Key {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "key_value", nullable = false, length = 16, unique = true)
    private String keyValue;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Key() {}

    public Key(String keyValue) {
        this.keyValue = keyValue;
        this.isUsed = false;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public Boolean getIsUsed() {
        return isUsed;
    }

    public void setIsUsed(Boolean isUsed) {
        this.isUsed = isUsed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
