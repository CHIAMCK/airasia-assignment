package com.example.shortener.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Value("${rate-limit.anonymous-shorten.capacity:10}")
    private int anonymousShortenCapacity;

    @Value("${rate-limit.anonymous-shorten.refill-per-second:10}")
    private int anonymousShortenRefillPerSecond;

    @Bean
    public Bucket anonymousShortenBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(anonymousShortenCapacity)
                .refillGreedy(anonymousShortenRefillPerSecond, Duration.ofSeconds(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
