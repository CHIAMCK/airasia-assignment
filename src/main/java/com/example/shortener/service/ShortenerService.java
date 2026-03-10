package com.example.shortener.service;

import com.example.shortener.entity.User;
import com.example.shortener.entity.UrlMapping;
import com.example.shortener.repository.UrlMappingRepository;
import com.example.shortener.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@Service
public class ShortenerService {

    @Value("${shortener.base-url:http://localhost:8080}")
    private String shortBaseUrl;

    @Value("${shortener.max-retries:3}")
    private int maxRetries;

    private final UrlMappingRepository urlMappingRepository;
    private final UserRepository userRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public ShortenerService(UrlMappingRepository urlMappingRepository, UserRepository userRepository,
                           SnowflakeIdGenerator snowflakeIdGenerator) {
        this.urlMappingRepository = urlMappingRepository;
        this.userRepository = userRepository;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    /**
     * Normalizes a long URL by parsing it, then re-encoding to a canonical form.
     * Handles mixed or unencoded input and ensures the result is safe for redirect (Location header).
     *
     * @throws IllegalArgumentException if the URL is invalid
     */
    public String normalizeLongUrl(String longUrl) {
        if (longUrl == null || longUrl.isBlank()) {
            throw new IllegalArgumentException("URL is required");
        }

        try {
            URI uri = URI.create(longUrl.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalArgumentException("Invalid URL: scheme and host required");
            }

            String path = uri.getPath();
            if (path == null) {
                path = "";
            }

            String query = uri.getQuery();
            boolean queryEmpty = (query == null || query.isEmpty());
            int questionMarkIndex = path.indexOf('?');
            if (queryEmpty && questionMarkIndex >= 0) {
                query = path.substring(questionMarkIndex + 1);
                path = path.substring(0, questionMarkIndex);
            }

            String queryParam = null;
            if (query != null && !query.isEmpty()) {
                queryParam = query;
            }
 
            URI normalized = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
                    path, queryParam, uri.getFragment());
            return normalized.toASCIIString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + e.getMessage(), e);
        }
    }

    @Transactional
    public String shorten(String longUrl, Long userId) {
        String originalUrl = normalizeLongUrl(longUrl);
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        }

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            String code = snowflakeIdGenerator.generateShortCode();
            UrlMapping mapping = new UrlMapping(code, originalUrl, user);
            try {
                urlMappingRepository.save(mapping);
                return shortBaseUrl + "/r/" + code;
            } catch (DataIntegrityViolationException e) {
                // DB unique constraint safety guard: rare duplicate (clock skew, etc.), retry with new ID
                if (attempt == maxRetries - 1) {
                    throw e;
                }
            }
        }

        throw new IllegalStateException("Failed to generate unique short code after " + maxRetries + " attempts");
    }

    @Cacheable(value = "urlMappings", key = "#shortCode")
    public Optional<String> resolve(@NonNull String shortCode) {
        if (shortCode.isEmpty()) {
            return Optional.empty();
        }

        return urlMappingRepository.findById(shortCode)
                .map(UrlMapping::getOriginalUrl);
    }
}
