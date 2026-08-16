package com.tiny.url.service;

import com.tiny.url.dto.UrlResponse;
import com.tiny.url.entity.Url;
import com.tiny.url.exception.InvalidUrlException;
import com.tiny.url.exception.UrlNotFoundException;
import com.tiny.url.mapper.UrlMapper;
import com.tiny.url.repository.UrlRepository;
import com.tiny.url.util.CodeGenerator;
import com.tiny.url.util.Constants;
import com.tiny.url.util.UrlValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Domain service for shortening URLs and resolving short codes.
 */
@Slf4j
@Service
public class TinyUrlService {

    private static final int MAX_URL_LENGTH = 2048;

    private final UrlRepository urlRepository;
    private final CodeGenerator codeGenerator;
    private final UrlMapper urlMapper;
    private final RedisService redisService;
    private final SecureRandom secureRandom = new SecureRandom();
    private static final ConcurrentHashMap<String, Object> codeGenerationLocks = new ConcurrentHashMap<>();

    @Autowired
    public TinyUrlService(
            UrlRepository urlRepository,
            CodeGenerator codeGenerator,
            UrlMapper urlMapper,
            RedisService redisService) {
        this.urlRepository = urlRepository;
        this.codeGenerator = codeGenerator;
        this.urlMapper = urlMapper;
        this.redisService = redisService;
    }

    @Transactional
    public UrlResponse shortenUrl(String originalUrl) {
        log.debug("Processing URL shortening request for: {}", originalUrl);

        if (!StringUtils.hasText(originalUrl)) {
            throw new InvalidUrlException("URL cannot be empty");
        }

        try {
            validateUrl(originalUrl);
            String normalizedUrl = normalizeUrl(originalUrl);

            Optional<Url> cachedUrl = redisService.getByOriginalUrl(normalizedUrl);
            if (cachedUrl.isPresent()) {
                log.debug("Cache hit for URL: {}", normalizedUrl);
                return urlMapper.toUrlResponse(cachedUrl.get());
            }

            Url existingUrl = urlRepository.findByOriginalUrl(normalizedUrl);
            if (existingUrl != null) {
                log.debug("Found existing URL in database: {}", normalizedUrl);
                redisService.cacheUrl(existingUrl);
                return urlMapper.toUrlResponse(existingUrl);
            }

            Url newUrl = createNewShortenedUrl(normalizedUrl);
            log.info("Created new shortened URL: {}", newUrl.getTinyUrl());
            return urlMapper.toUrlResponse(newUrl);
        } catch (InvalidUrlException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing URL shortening request", e);
            throw new RuntimeException("Failed to process URL shortening request", e);
        }
    }

    public UrlResponse getUrl(String tinyUrl) {
        log.debug("Processing URL retrieval request for: {}", tinyUrl);

        if (!StringUtils.hasText(tinyUrl)) {
            throw new InvalidUrlException("Tiny URL cannot be empty");
        }

        try {
            Optional<Url> cachedUrl = redisService.getByTinyUrl(tinyUrl);
            if (cachedUrl.isPresent()) {
                updateUrlStats(cachedUrl.get());
                return urlMapper.toUrlResponse(cachedUrl.get());
            }

            Url url = urlRepository.findByTinyUrl(tinyUrl);
            if (url == null) {
                throw new UrlNotFoundException("URL not found for: " + tinyUrl);
            }

            updateUrlStats(url);
            redisService.cacheUrl(url);
            return urlMapper.toUrlResponse(url);
        } catch (UrlNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving URL", e);
            throw new RuntimeException("Failed to retrieve URL", e);
        }
    }

    private String generateUniqueCode(String originalUrl) {
        int retryCount = 0;
        String code = codeGenerator.generateUniqueCode(originalUrl);

        while (retryCount < Constants.MAX_RETRIES) {
            Object lock = codeGenerationLocks.computeIfAbsent(code, k -> new Object());

            synchronized (lock) {
                try {
                    if (isCodeAvailable(code)) {
                        codeGenerationLocks.remove(code);
                        return code;
                    }
                    code = generateRandomCode();
                    retryCount++;
                } catch (Exception e) {
                    log.error("Error checking code availability: {}", code, e);
                    retryCount++;
                }
            }
        }

        log.error("Failed to generate unique code after {} attempts", Constants.MAX_RETRIES);
        throw new RuntimeException("Unable to generate unique code after maximum retries");
    }

    private void validateUrl(String url) {
        if (!UrlValidator.isValidUrl(url)) {
            throw new InvalidUrlException("Invalid URL format: " + url);
        }
        if (url.length() > MAX_URL_LENGTH) {
            throw new InvalidUrlException("URL exceeds maximum length of " + MAX_URL_LENGTH);
        }
    }

    private boolean isCodeAvailable(String code) {
        return urlRepository.findByTinyUrl(code) == null;
    }

    private String generateRandomCode() {
        StringBuilder code = new StringBuilder(Constants.CODE_LENGTH);
        for (int i = 0; i < Constants.CODE_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(Constants.BASE62_CHARACTERS.length());
            code.append(Constants.BASE62_CHARACTERS.charAt(randomIndex));
        }
        return code.toString();
    }

    private String normalizeUrl(String url) {
        // Intentionally pass-through for now to preserve existing short-code mappings.
        return url;
    }

    private void updateUrlStats(Url url) {
        try {
            redisService.incrementClickCount(url.getId());
        } catch (Exception e) {
            log.warn("Failed to update URL statistics", e);
        }
    }

    private Url createNewShortenedUrl(String normalizedUrl) {
        String code = generateUniqueCode(normalizedUrl);
        Url newUrl = Url.builder()
                .tinyUrl(code)
                .originalUrl(normalizedUrl)
                .build();

        Url savedUrl = urlRepository.save(newUrl);
        redisService.cacheUrl(savedUrl);
        return savedUrl;
    }
}
