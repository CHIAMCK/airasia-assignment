package com.example.shortener.service;

import com.example.shortener.entity.Key;
import com.example.shortener.repository.KeyRepository;
import com.example.shortener.repository.UrlMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class PreGeneratedKeyService {

    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int KEY_LENGTH = 6;

    private final KeyRepository keyRepository;
    private final UrlMappingRepository urlMappingRepository;
    private final Random random;
    
    // Thread-safe queue for unused keys (cache for fast access)
    private final Queue<String> unusedKeys = new ConcurrentLinkedQueue<>();
    
    // Thread-safe set to track used keys (cache for fast access)
    private final Set<String> usedKeys = ConcurrentHashMap.newKeySet();

    public PreGeneratedKeyService(KeyRepository keyRepository, UrlMappingRepository urlMappingRepository) {
        this.keyRepository = keyRepository;
        this.urlMappingRepository = urlMappingRepository;
        this.random = new SecureRandom();
    }

    /**
     * Gets the next unused key from the pool.
     * First checks in-memory cache, then database.
     * Returns Optional.empty() if no keys are available.
     * Note: The key should be marked as used via markKeyAsUsed() after the URL mapping is created.
     */
    @Transactional
    public Optional<String> getNextKey() {
        String key = unusedKeys.poll();
        if (key != null) {
            return Optional.of(key);
        }
 
        Optional<Key> dbKey = keyRepository.findFirstByIsUsedFalseOrderByCreatedAtAsc();
        if (dbKey.isPresent()) {
            key = dbKey.get().getKeyValue();
            return Optional.of(key);
        }
        
        return Optional.empty();
    }
    
    /**
     * Marks a key as used in both database and in-memory cache.
     * Should be called after the URL mapping is successfully created.
     */
    @Transactional
    public void markKeyAsUsed(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }
        usedKeys.add(key);
        keyRepository.findByKeyValue(key).ifPresent(keyEntity -> {
            if (!keyEntity.getIsUsed()) {
                keyEntity.setIsUsed(true);
                keyRepository.save(keyEntity);
            }
        });
    }

    /**
     * Adds keys to the unused keys pool.
     * Filters out reserved codes and already-used keys (both in-memory and database).
     * Persists new keys to database.
     */
    @Transactional
    public void addKeys(Collection<String> keys) {
        for (String key : keys) {
            if (key != null && !key.isEmpty() 
                    && !usedKeys.contains(key)
                    && !keyRepository.existsByKeyValue(key)
                    && !urlMappingRepository.existsById(key)) {
                // Save to database
                Key keyEntity = new Key(key);
                keyRepository.save(keyEntity);
                // Also add to in-memory cache for fast access
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
            int index = random.nextInt(62);
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
     * Returns a set of keys that are duplicates (either in keys table or url_mapping table).
     */
    public Set<String> checkDuplicates(Collection<String> keys) {
        if (keys.isEmpty()) {
            return Collections.emptySet();
        }
        
        // Check each key against both databases
        Set<String> duplicates = new HashSet<>();
        for (String key : keys) {
            if (keyRepository.existsByKeyValue(key) || urlMappingRepository.existsById(key)) {
                duplicates.add(key);
            }
        }
        return duplicates;
    }

    /**
     * Gets the current size of the unused keys pool (in-memory cache).
     * Note: This only returns the cache size. Use getDatabasePoolSize() for total unused keys.
     */
    public int getPoolSize() {
        return unusedKeys.size();
    }
    
    /**
     * Gets the count of unused keys in the database.
     */
    public long getDatabasePoolSize() {
        return keyRepository.countUnusedKeys();
    }
}
