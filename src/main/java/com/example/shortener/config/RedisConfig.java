package com.example.shortener.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.lang.NonNull;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${cache.url-mappings.ttl:86400}")
    private long urlMappingsTtlSeconds;

    @Bean
    @SuppressWarnings("null")
    public CacheManager cacheManager(@NonNull RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration urlMappingsConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(urlMappingsTtlSeconds));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig())
                .withCacheConfiguration("urlMappings", urlMappingsConfig)
                .build();
    }
}
