package com.example.shortener.service;

import com.example.shortener.entity.User;
import com.example.shortener.entity.UrlMapping;
import com.example.shortener.repository.UrlMappingRepository;
import com.example.shortener.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@Service
public class ShortenerService {
    
    private static final String SHORT_BASE_URL = "https://short.ly";
    
    private final UrlMappingRepository urlMappingRepository;
    private final UserRepository userRepository;
    private final PreGeneratedKeyService preGeneratedKeyService;

    public ShortenerService(UrlMappingRepository urlMappingRepository, UserRepository userRepository,
                           PreGeneratedKeyService preGeneratedKeyService) {
        this.urlMappingRepository = urlMappingRepository;
        this.userRepository = userRepository;
        this.preGeneratedKeyService = preGeneratedKeyService;
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
    public String shorten(String longUrl, String requestBaseUrl, Long userId) {
        String originalUrl = normalizeLongUrl(longUrl);
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        }

        String code = null;
        Optional<String> preGeneratedKey = preGeneratedKeyService.getNextKey();
        if (preGeneratedKey.isPresent()) {
            code = preGeneratedKey.get();
        }
        
        if (code == null || code.isEmpty()) {
            code = generateKeyOnDemand();
        }

        UrlMapping mapping = new UrlMapping(code, originalUrl, user);
        urlMappingRepository.save(mapping);
        
        preGeneratedKeyService.markKeyAsUsed(code);
        return SHORT_BASE_URL + "/r/" + code;
    }

    public Optional<String> resolve(String shortCode) {
        return urlMappingRepository.findById(shortCode)
                .map(UrlMapping::getOriginalUrl);
    }

    /**
     * Generates a key on-demand when the pre-generated pool is empty.
     */
    private String generateKeyOnDemand() {
        String code;
        int maxRetries = 3;
        int retryCount = 0;
        
        do {
            code = preGeneratedKeyService.generateRandomKey();
            retryCount++;
            
            if (code != null && !code.isEmpty() && urlMappingRepository.existsById(code)) {
                code = null;
            }

        } while (code == null && retryCount < maxRetries);
        
        if (code == null) {
            throw new IllegalStateException("Failed to generate unique key after " + maxRetries + " attempts");
        }
        
        return code;
    }
}
