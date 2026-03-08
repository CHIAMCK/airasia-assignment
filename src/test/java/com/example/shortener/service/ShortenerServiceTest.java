package com.example.shortener.service;

import com.example.shortener.entity.User;
import com.example.shortener.entity.UrlMapping;
import com.example.shortener.repository.UrlMappingRepository;
import com.example.shortener.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortenerServiceTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    private ShortenerService shortenerService;

    @BeforeEach
    void setUp() {
        shortenerService = new ShortenerService(urlMappingRepository, userRepository, snowflakeIdGenerator);
    }

    @Nested
    @DisplayName("normalizeLongUrl")
    class NormalizeLongUrlTests {

        @Test
        @DisplayName("throws when URL is null")
        void throwsWhenUrlIsNull() {
            assertThatThrownBy(() -> shortenerService.normalizeLongUrl(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("URL is required");
        }

        @Test
        @DisplayName("throws when URL is blank")
        void throwsWhenUrlIsBlank() {
            assertThatThrownBy(() -> shortenerService.normalizeLongUrl("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("URL is required");
        }

        @Test
        @DisplayName("throws when URL has no scheme")
        void throwsWhenUrlHasNoScheme() {
            assertThatThrownBy(() -> shortenerService.normalizeLongUrl("example.com/path"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("scheme and host required");
        }

        @Test
        @DisplayName("throws when URL has no host")
        void throwsWhenUrlHasNoHost() {
            assertThatThrownBy(() -> shortenerService.normalizeLongUrl("http:///path"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("scheme and host required");
        }

        @Test
        @DisplayName("normalizes simple HTTP URL")
        void normalizesSimpleHttpUrl() {
            String result = shortenerService.normalizeLongUrl("http://example.com");
            assertThat(result).isEqualTo("http://example.com");
        }

        @Test
        @DisplayName("normalizes URL with path")
        void normalizesUrlWithPath() {
            String result = shortenerService.normalizeLongUrl("https://example.com/path/to/page");
            assertThat(result).isEqualTo("https://example.com/path/to/page");
        }

        @Test
        @DisplayName("normalizes URL with query string")
        void normalizesUrlWithQueryString() {
            String result = shortenerService.normalizeLongUrl("https://example.com/search?q=test&page=1");
            assertThat(result).isEqualTo("https://example.com/search?q=test&page=1");
        }

        @Test
        @DisplayName("trims whitespace from input")
        void trimsWhitespaceFromInput() {
            String result = shortenerService.normalizeLongUrl("  https://example.com  ");
            assertThat(result).isEqualTo("https://example.com");
        }

        @Test
        @DisplayName("normalizes URL with port")
        void normalizesUrlWithPort() {
            String result = shortenerService.normalizeLongUrl("http://example.com:8080/api");
            assertThat(result).isEqualTo("http://example.com:8080/api");
        }

        @Test
        @DisplayName("throws for invalid URL syntax")
        void throwsForInvalidUrlSyntax() {
            assertThatThrownBy(() -> shortenerService.normalizeLongUrl("http://[invalid"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("shorten")
    class ShortenTests {

        @Test
        @DisplayName("returns short URL for valid long URL without user")
        void returnsShortUrlForValidLongUrlWithoutUser() {
            when(snowflakeIdGenerator.generateShortCode()).thenReturn("abc123");
            when(urlMappingRepository.save(any(UrlMapping.class))).thenAnswer(inv -> inv.getArgument(0));

            String result = shortenerService.shorten("https://example.com/page", null);

            assertThat(result).isEqualTo("http://localhost:8080/r/abc123");
            verify(urlMappingRepository).save(any(UrlMapping.class));
        }

        @Test
        @DisplayName("returns short URL for valid long URL with user")
        void returnsShortUrlForValidLongUrlWithUser() {
            User user = new User("Alice", "alice@example.com");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(snowflakeIdGenerator.generateShortCode()).thenReturn("xyz789");
            when(urlMappingRepository.save(any(UrlMapping.class))).thenAnswer(inv -> inv.getArgument(0));

            String result = shortenerService.shorten("https://example.com", 1L);

            assertThat(result).isEqualTo("http://localhost:8080/r/xyz789");
            verify(urlMappingRepository).save(any(UrlMapping.class));
        }

        @Test
        @DisplayName("throws when user not found")
        void throwsWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> shortenerService.shorten("https://example.com", 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found: 999");
        }

        @Test
        @DisplayName("throws when long URL is invalid")
        void throwsWhenLongUrlIsInvalid() {
            assertThatThrownBy(() -> shortenerService.shorten("not-a-url", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("retries on DataIntegrityViolationException and succeeds")
        void retriesOnDuplicateAndSucceeds() {
            when(snowflakeIdGenerator.generateShortCode())
                    .thenReturn("dup1")
                    .thenReturn("unique");
            when(urlMappingRepository.save(any(UrlMapping.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate"))
                    .thenAnswer(inv -> inv.getArgument(0));

            String result = shortenerService.shorten("https://example.com", null);

            assertThat(result).isEqualTo("http://localhost:8080/r/unique");
        }

        @Test
        @DisplayName("throws when DataIntegrityViolationException persists after max retries")
        void throwsWhenDuplicatePersistsAfterMaxRetries() {
            when(snowflakeIdGenerator.generateShortCode()).thenReturn("dup1");
            when(urlMappingRepository.save(any(UrlMapping.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate"));

            assertThatThrownBy(() -> shortenerService.shorten("https://example.com", null))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("resolve")
    class ResolveTests {

        @Test
        @DisplayName("returns empty when short code is empty")
        void returnsEmptyWhenShortCodeIsEmpty() {
            Optional<String> result = shortenerService.resolve("");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns original URL when mapping exists")
        void returnsOriginalUrlWhenMappingExists() {
            UrlMapping mapping = new UrlMapping("abc123", "https://example.com/original");
            when(urlMappingRepository.findById("abc123")).thenReturn(Optional.of(mapping));

            Optional<String> result = shortenerService.resolve("abc123");

            assertThat(result).hasValue("https://example.com/original");
        }

        @Test
        @DisplayName("returns empty when mapping does not exist")
        void returnsEmptyWhenMappingDoesNotExist() {
            when(urlMappingRepository.findById("nonexistent")).thenReturn(Optional.empty());

            Optional<String> result = shortenerService.resolve("nonexistent");

            assertThat(result).isEmpty();
        }
    }
}
