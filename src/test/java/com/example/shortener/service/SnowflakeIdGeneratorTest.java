package com.example.shortener.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SnowflakeIdGeneratorTest {

    private static final Pattern BASE62_PATTERN = Pattern.compile("^[0-9A-Za-z]+$");

    @Nested
    @DisplayName("generateShortCode")
    class GenerateShortCodeTests {

        @Test
        @DisplayName("returns non-empty string")
        void returnsNonEmptyString() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator(0);

            String code = generator.generateShortCode();

            assertThat(code).isNotEmpty();
        }

        @Test
        @DisplayName("returns only base62 characters")
        void returnsOnlyBase62Characters() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator(0);

            String code = generator.generateShortCode();

            assertThat(code).matches(BASE62_PATTERN);
        }

        @Test
        @DisplayName("returns different codes on consecutive calls")
        void returnsDifferentCodesOnConsecutiveCalls() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator(0);
            Set<String> codes = new HashSet<>();

            for (int i = 0; i < 100; i++) {
                codes.add(generator.generateShortCode());
            }

            assertThat(codes).hasSize(100);
        }

        @Test
        @DisplayName("returns short codes suitable for URLs")
        void returnsShortCodesSuitableForUrls() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator(0);

            String code = generator.generateShortCode();

            assertThat(code).doesNotContain("+", "/", "=");
            assertThat(code.length()).isLessThanOrEqualTo(12);
        }
    }
}
