package com.example.shortener.service;

import com.relops.snowflake.Snowflake;
import org.unbrokendome.base62.Base62;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Generates unique short codes using Snowflake IDs with relative timestamp encoding.
 * Encodes relative_id = snowflake_id - epoch_start to base62 for shorter URLs.
 */
@Service
public class SnowflakeIdGenerator {

    /**
     * Snowflake ID at 2024-01-01 00:00:00 UTC (node 0, seq 0).
     * Format: timestamp << 22 | node << 12 | sequence
     */
    private static final long EPOCH_START = 1704067200000L << 22;

    private final Snowflake snowflake;

    public SnowflakeIdGenerator(@Value("${snowflake.machine-id:0}") int machineId) {
        this.snowflake = new Snowflake(machineId);
    }

    /**
     * Generates a unique short code by encoding (snowflake_id - epoch_start) to base62.
     * Early IDs produce 1-2 character codes; length grows over time.
     */
    public String generateShortCode() {
        long snowflakeId = snowflake.next();
        long relativeId = snowflakeId - EPOCH_START;
        if (relativeId < 0) {
            throw new IllegalStateException("Clock skew: snowflake ID is before epoch");
        }

        return Base62.encode(relativeId);
    }
}
