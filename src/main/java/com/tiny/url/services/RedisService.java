package com.tiny.url.services;

import com.tiny.url.models.Url;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
public class RedisService {
    private static final String URL_CACHE_PREFIX = "url:";
    private static final String ANALYTICS_CACHE_PREFIX = "analytics:";
    private static final Duration DEFAULT_CACHE_DURATION = Duration.ofHours(24);
    private static final Duration EXTENDED_CACHE_DURATION = Duration.ofDays(7);
    
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<Url> getUrl(String key, String prefix) {
        try {
            String cacheKey = generateKey(prefix, key);
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue instanceof Url) {
                log.debug("Cache hit for key: {}", cacheKey);
                return Optional.of((Url) cachedValue);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving from Redis cache: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void cacheUrl(Url url) {
        try {
            // Cache by tiny URL
            String tinyUrlKey = generateKey(URL_CACHE_PREFIX, "tiny:", url.getTinyUrl());
            redisTemplate.opsForValue().set(tinyUrlKey, url, DEFAULT_CACHE_DURATION);

            // Cache by original URL
            String originalUrlKey = generateKey(URL_CACHE_PREFIX, "original:", url.getOriginalUrl());
            redisTemplate.opsForValue().set(originalUrlKey, url, DEFAULT_CACHE_DURATION);

            // Update access count in Redis
            String accessCountKey = generateKey(ANALYTICS_CACHE_PREFIX, "access:", url.getId());
            redisTemplate.opsForValue().increment(accessCountKey);

            // If URL is frequently accessed, extend its cache duration
            Long accessCount = redisTemplate.opsForValue().get(accessCountKey) != null ? 
                Long.parseLong(redisTemplate.opsForValue().get(accessCountKey).toString()) : 0;
            
            if (accessCount > 100) {
                redisTemplate.expire(tinyUrlKey, EXTENDED_CACHE_DURATION);
                redisTemplate.expire(originalUrlKey, EXTENDED_CACHE_DURATION);
            }

            log.debug("Successfully cached URL with key: {}", tinyUrlKey);
        } catch (Exception e) {
            log.error("Error caching URL: {}", e.getMessage());
        }
    }

    public void invalidateCache(String key) {
        try {
            String tinyUrlKey = generateKey(URL_CACHE_PREFIX, "tiny:", key);
            String originalUrlKey = generateKey(URL_CACHE_PREFIX, "original:", key);
            
            redisTemplate.delete(tinyUrlKey);
            redisTemplate.delete(originalUrlKey);
            
            log.debug("Successfully invalidated cache for key: {}", key);
        } catch (Exception e) {
            log.error("Error invalidating cache: {}", e.getMessage());
        }
    }

    public void incrementClickCount(String urlId) {
        try {
            String clickCountKey = generateKey(ANALYTICS_CACHE_PREFIX, "clicks:", urlId);
            redisTemplate.opsForValue().increment(clickCountKey);
            redisTemplate.expire(clickCountKey, Duration.ofDays(30));
        } catch (Exception e) {
            log.error("Error incrementing click count: {}", e.getMessage());
        }
    }

    private String generateKey(String... parts) {
        return String.join(":", parts);
    }
} 