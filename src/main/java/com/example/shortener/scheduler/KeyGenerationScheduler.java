package com.example.shortener.scheduler;

import com.example.shortener.service.PreGeneratedKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class KeyGenerationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(KeyGenerationScheduler.class);
    private static final int TARGET_KEY_COUNT = 100;
    private static final int MAX_RETRIES = 10; // Prevent infinite loops

    private final PreGeneratedKeyService preGeneratedKeyService;

    public KeyGenerationScheduler(PreGeneratedKeyService preGeneratedKeyService) {
        this.preGeneratedKeyService = preGeneratedKeyService;
    }

    /**
     * Scheduled task that runs every 5 minutes to generate 100 random keys.
     * Checks for duplicates and ensures exactly 100 unique keys are added to the pool.
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void generateKeys() {
        logger.info("Starting scheduled key generation");
        
        List<String> uniqueKeys = new ArrayList<>();
        int retryCount = 0;
        
        while (uniqueKeys.size() < TARGET_KEY_COUNT && retryCount < MAX_RETRIES) {
            // Generate batch of keys
            int remaining = TARGET_KEY_COUNT - uniqueKeys.size();
            List<String> candidateKeys = preGeneratedKeyService.generateBatch(remaining);
            
            // Check for duplicates in database
            Set<String> duplicates = preGeneratedKeyService.checkDuplicates(candidateKeys);
            
            // Filter out duplicates
            List<String> newUniqueKeys = candidateKeys.stream()
                    .filter(key -> !duplicates.contains(key))
                    .collect(java.util.stream.Collectors.toList());
            
            uniqueKeys.addAll(newUniqueKeys);
            
            if (uniqueKeys.size() < TARGET_KEY_COUNT) {
                retryCount++;
                logger.debug("Generated {} unique keys so far, need {} more. Retry count: {}", 
                        uniqueKeys.size(), TARGET_KEY_COUNT - uniqueKeys.size(), retryCount);
            }
        }
        
        if (uniqueKeys.size() < TARGET_KEY_COUNT) {
            logger.warn("Could only generate {} unique keys out of {} requested after {} retries", 
                    uniqueKeys.size(), TARGET_KEY_COUNT, retryCount);
        }
        
        // Add unique keys to the pool
        preGeneratedKeyService.addKeys(uniqueKeys);
        
        logger.info("Completed scheduled key generation. Added {} keys to pool. Pool size: {}", 
                uniqueKeys.size(), preGeneratedKeyService.getPoolSize());
    }
}
