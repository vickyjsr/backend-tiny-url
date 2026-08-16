package com.tiny.url.service;

import com.tiny.url.entity.Url;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed cache for shortened URLs and lightweight click counters.
 * Key format (must stay consistent between read and write):
 *   url:tiny:{code}
 *   url:original:{originalUrl}
 *   analytics:clicks:{urlId}
 */
@Slf4j
@Service
public class RedisService {

    private static final String TINY_KEY_PREFIX = "url:tiny:";
    private static final String ORIGINAL_KEY_PREFIX = "url:original:";
    private static final String CLICK_KEY_PREFIX = "analytics:clicks:";
    private static final Duration DEFAULT_CACHE_DURATION = Duration.ofHours(24);
    private static final Duration EXTENDED_CACHE_DURATION = Duration.ofDays(7);
    private static final long HOT_URL_CLICK_THRESHOLD = 100;

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<Url> getByTinyUrl(String tinyUrl) {
        return getUrl(TINY_KEY_PREFIX + tinyUrl);
    }

    public Optional<Url> getByOriginalUrl(String originalUrl) {
        return getUrl(ORIGINAL_KEY_PREFIX + originalUrl);
    }

    public void cacheUrl(Url url) {
        try {
            String tinyUrlKey = TINY_KEY_PREFIX + url.getTinyUrl();
            String originalUrlKey = ORIGINAL_KEY_PREFIX + url.getOriginalUrl();

            redisTemplate.opsForValue().set(tinyUrlKey, url, DEFAULT_CACHE_DURATION);
            redisTemplate.opsForValue().set(originalUrlKey, url, DEFAULT_CACHE_DURATION);

            String clickCountKey = CLICK_KEY_PREFIX + url.getId();
            Long accessCount = redisTemplate.opsForValue().increment(clickCountKey);
            if (accessCount != null && accessCount > HOT_URL_CLICK_THRESHOLD) {
                redisTemplate.expire(tinyUrlKey, EXTENDED_CACHE_DURATION);
                redisTemplate.expire(originalUrlKey, EXTENDED_CACHE_DURATION);
            }

            log.debug("Cached URL under keys {} and {}", tinyUrlKey, originalUrlKey);
        } catch (Exception e) {
            log.error("Error caching URL: {}", e.getMessage());
        }
    }

    public void invalidateCache(String tinyUrl, String originalUrl) {
        try {
            redisTemplate.delete(TINY_KEY_PREFIX + tinyUrl);
            redisTemplate.delete(ORIGINAL_KEY_PREFIX + originalUrl);
            log.debug("Invalidated cache for tinyUrl={}", tinyUrl);
        } catch (Exception e) {
            log.error("Error invalidating cache: {}", e.getMessage());
        }
    }

    public void incrementClickCount(String urlId) {
        try {
            String clickCountKey = CLICK_KEY_PREFIX + urlId;
            redisTemplate.opsForValue().increment(clickCountKey);
            redisTemplate.expire(clickCountKey, Duration.ofDays(30));
        } catch (Exception e) {
            log.error("Error incrementing click count: {}", e.getMessage());
        }
    }

    private Optional<Url> getUrl(String cacheKey) {
        try {
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue instanceof Url url) {
                log.debug("Cache hit for key: {}", cacheKey);
                return Optional.of(url);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving from Redis cache: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
