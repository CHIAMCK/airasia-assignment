package com.example.shortener.service;

import com.example.shortener.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Nested
    @DisplayName("consumeAnonymousShorten")
    class ConsumeAnonymousShortenTests {

        @Test
        @DisplayName("succeeds when bucket has capacity")
        void succeedsWhenBucketHasCapacity() {
            Bucket bucket = Bucket.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(10)
                            .refillGreedy(10, Duration.ofSeconds(1))
                            .build())
                    .build();
            RateLimitService service = new RateLimitService(bucket);

            assertThatCode(() -> service.consumeAnonymousShorten())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("succeeds multiple times within capacity")
        void succeedsMultipleTimesWithinCapacity() {
            Bucket bucket = Bucket.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(5)
                            .refillGreedy(5, Duration.ofSeconds(1))
                            .build())
                    .build();
            RateLimitService service = new RateLimitService(bucket);

            assertThatCode(() -> {
                for (int i = 0; i < 5; i++) {
                    service.consumeAnonymousShorten();
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("throws RateLimitExceededException when bucket exhausted")
        void throwsWhenBucketExhausted() {
            Bucket bucket = Bucket.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(1)
                            .refillGreedy(1, Duration.ofSeconds(1))
                            .build())
                    .build();
            RateLimitService service = new RateLimitService(bucket);

            service.consumeAnonymousShorten();

            assertThatThrownBy(() -> service.consumeAnonymousShorten())
                    .isInstanceOf(RateLimitExceededException.class)
                    .hasMessageContaining("Rate limit exceeded");
        }

        @Test
        @DisplayName("RateLimitExceededException includes retry-after seconds")
        void rateLimitExceptionIncludesRetryAfterSeconds() {
            Bucket bucket = Bucket.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(1)
                            .refillGreedy(1, Duration.ofSeconds(60))
                            .build())
                    .build();
            RateLimitService service = new RateLimitService(bucket);

            service.consumeAnonymousShorten();

            assertThatThrownBy(() -> service.consumeAnonymousShorten())
                    .isInstanceOf(RateLimitExceededException.class)
                    .satisfies(ex -> assertThat(((RateLimitExceededException) ex).getRetryAfterSeconds())
                            .isGreaterThanOrEqualTo(1));
        }
    }
}
