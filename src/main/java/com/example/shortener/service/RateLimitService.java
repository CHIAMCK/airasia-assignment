package com.example.shortener.service;

import com.example.shortener.exception.RateLimitExceededException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final Bucket anonymousShortenBucket;

    public RateLimitService(Bucket anonymousShortenBucket) {
        this.anonymousShortenBucket = anonymousShortenBucket;
    }

    public void consumeAnonymousShorten() {
        ConsumptionProbe probe = anonymousShortenBucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long nanosToWait = probe.getNanosToWaitForRefill();
            long secondsToWait = nanosToWait > 0 ? (nanosToWait / 1_000_000_000) + 1 : 1;
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Try again later.",
                    secondsToWait
            );
        }
    }
}
