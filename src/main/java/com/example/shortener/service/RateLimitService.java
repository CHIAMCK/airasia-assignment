package com.example.shortener.service;

import com.example.shortener.exception.RateLimitExceededException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

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
            long secondsToWait = calculateSecondsToWait(nanosToWait);
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Try again later.",
                    secondsToWait
            );
        }
    }

    private long calculateSecondsToWait(long nanosToWait) {
        if (nanosToWait <= 0) {
            return 1;
        }

        return TimeUnit.NANOSECONDS.toSeconds(nanosToWait) + 1;
    }
}
