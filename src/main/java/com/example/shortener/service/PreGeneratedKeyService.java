package com.example.shortener.service;

import com.example.shortener.repository.UrlMappingRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Optional;

@Service
public class PreGeneratedKeyService {

    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String RESERVED_CODE = "shorten";
    private static final int KEY_LENGTH = 6;

    private final UrlMappingRepository urlMappingRepository;
    private final Random random;
    
    // Thread-safe queue for unused keys
    private final Queue<String> unusedKeys = new ConcurrentLinkedQueue<>();
    
    // Thread-safe set to track used keys
    private final Set<String> usedKeys = ConcurrentHashMap.newKeySet();

    public PreGeneratedKeyService(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
        this.random = new SecureRandom();
    }

    /**
     * Gets the next unused key from the pool and marks it as used.
     * Returns Optional.empty() if no keys are available.
     */
    public Optional<String> getNextKey() {
        String key = unusedKeys.poll();
        if (key != null) {
            usedKeys.add(key);
            return Optional.of(key);
        }
        return Optional.empty();
    }

    /**
     * Adds keys to the unused keys pool.
     * Filters out reserved codes and already-used keys.
     */
    public void addKeys(Collection<String> keys) {
        for (String key : keys) {
            if (!RESERVED_CODE.equals(key) && !usedKeys.contains(key)) {
                unusedKeys.offer(key);
            }
        }
    }

    /**
     * Generates a single random 6-character base62 key.
     */
    public String generateRandomKey() {
        StringBuilder key = new StringBuilder(KEY_LENGTH);
        for (int i = 0; i < KEY_LENGTH; i++) {
            int index = random.nextInt(62); // 0-61
            key.append(BASE62.charAt(index));
        }
        return key.toString();
    }

    /**
     * Generates a batch of random keys.
     */
    public List<String> generateBatch(int count) {
        List<String> keys = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            keys.add(generateRandomKey());
        }
        return keys;
    }

    /**
     * Checks which keys from the collection already exist in the database.
     * Returns a set of keys that are duplicates.
     */
    public Set<String> checkDuplicates(Collection<String> keys) {
        if (keys.isEmpty()) {
            return Collections.emptySet();
        }
        
        // Check each key against the database
        Set<String> duplicates = new HashSet<>();
        for (String key : keys) {
            if (urlMappingRepository.findById(key).isPresent()) {
                duplicates.add(key);
            }
        }
        return duplicates;
    }

    /**
     * Gets the current size of the unused keys pool.
     */
    public int getPoolSize() {
        return unusedKeys.size();
    }
}
