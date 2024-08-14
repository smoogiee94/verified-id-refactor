package com.smoogiee.verifiedidbackend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service class used for calls into an in-memory Caffeine cache instance
 */
@Slf4j
@Service
public class CacheService<K, V> {
    private final Cache<K, V> cache;

    /**
     * Constructor
     */
    public CacheService() {
        this.cache = Caffeine
                .newBuilder()
                .expireAfterWrite(
                        15,
                        TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }

    /**
     * Get the value cached by the provided key
     *
     * @param key The key to search the cache for
     * @return The value cached by the provided key or null if key was not found
     */
    public V getIfPresent(K key) {
        return cache.getIfPresent(key);
    }

    /**
     * Store the value into the cache using the provided key
     *
     * @param key The key to cache the value under
     * @param value The value to cache
     */
    public void put(K key, V value) {
        cache.put(key, value);
    }
}
