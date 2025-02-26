package com.tiny.url.services;

import com.tiny.url.constants.Constants;
import com.tiny.url.exception.InvalidUrlException;
import com.tiny.url.exception.UrlNotFoundException;
import com.tiny.url.helpers.CodeGenerator;
import com.tiny.url.models.Url;
import com.tiny.url.repository.UrlRepository;
import com.tiny.url.dto.UrlResponse;
import com.tiny.url.adapter.UrlAdapter;
import com.tiny.url.utils.UrlValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.Optional;

/**
 * Service class for handling URL shortening operations.
 * This service provides functionality to shorten URLs and retrieve original URLs.
 */
@Slf4j
@Service
public class TinyUrlService {

    private final UrlRepository urlRepository;
    private final CodeGenerator codeGenerator;
    private final UrlAdapter urlAdapter;
    private final RedisService redisService;
    private static final String URL_CACHE_PREFIX = "url:";
    private static final long CACHE_DURATION = 24; // hours
    private static final int MAX_URL_LENGTH = 2048; // Standard max URL length
    private static final ConcurrentHashMap<String, Object> codeGenerationLocks = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public TinyUrlService(
            UrlRepository urlRepository, 
            CodeGenerator codeGenerator, 
            UrlAdapter urlAdapter,
            RedisService redisService) {
        this.urlRepository = urlRepository;
        this.codeGenerator = codeGenerator;
        this.urlAdapter = urlAdapter;
        this.redisService = redisService;
    }

    /**
     * Shortens a given URL with improved validation and error handling.
     *
     * @param originalUrl the URL to be shortened
     * @return UrlResponse object containing status code and URL information
     * @throws InvalidUrlException if the URL is invalid
     */
    @Transactional
    public UrlResponse shortenUrl(String originalUrl) {
        log.debug("Processing URL shortening request for: {}", originalUrl);
        
        if (!StringUtils.hasText(originalUrl)) {
            throw new InvalidUrlException("URL cannot be empty");
        }

        try {
            validateUrl(originalUrl);
            String normalizedUrl = normalizeUrl(originalUrl);
            
            // Check cache
            Optional<Url> cachedUrl = redisService.getUrl(normalizedUrl, "original");
            if (cachedUrl.isPresent()) {
                log.debug("Cache hit for URL: {}", normalizedUrl);
                return urlAdapter.toUrlResponse(cachedUrl.get());
            }

            // Check database
            Url existingUrl = urlRepository.findByOriginalUrl(normalizedUrl);
            if (existingUrl != null) {
                log.debug("Found existing URL in database: {}", normalizedUrl);
                redisService.cacheUrl(existingUrl);
                return urlAdapter.toUrlResponse(existingUrl);
            }

            // Create new shortened URL
            Url newUrl = createNewShortenedUrl(normalizedUrl);
            log.info("Created new shortened URL: {}", newUrl.getTinyUrl());
            return urlAdapter.toUrlResponse(newUrl);

        } catch (InvalidUrlException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing URL shortening request", e);
            throw new RuntimeException("Failed to process URL shortening request", e);
        }
    }

    /**
     * Retrieves the original URL for a given shortened URL code.
     *
     * @param tinyUrl the shortened URL code
     * @return UrlResponse object containing status code and URL information
     * @throws UrlNotFoundException if the shortened URL is not found
     */
    public UrlResponse getUrl(String tinyUrl) {
        log.debug("Processing URL retrieval request for: {}", tinyUrl);
        
        if (!StringUtils.hasText(tinyUrl)) {
            throw new InvalidUrlException("Tiny URL cannot be empty");
        }

        try {
            // Check cache
            Optional<Url> cachedUrl = redisService.getUrl(tinyUrl, "tiny");
            if (cachedUrl.isPresent()) {
                updateUrlStats(cachedUrl.get());
                return urlAdapter.toUrlResponse(cachedUrl.get());
            }

            // Check database
            Url url = urlRepository.findByTinyUrl(tinyUrl);
            if (url == null) {
                throw new UrlNotFoundException("URL not found for: " + tinyUrl);
            }

            updateUrlStats(url);
            redisService.cacheUrl(url);
            return urlAdapter.toUrlResponse(url);

        } catch (UrlNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving URL", e);
            throw new RuntimeException("Failed to retrieve URL", e);
        }
    }

    /**
     * Generates a unique code for the URL with improved collision handling and security.
     *
     * @param originalUrl the original URL to generate code for
     * @return unique code for the URL
     * @throws RuntimeException if unable to generate unique code after max retries
     */
    private String generateUniqueCode(String originalUrl) {
        int retryCount = 0;
        String code;
        
        // Use the first attempt with consistent hashing
        code = codeGenerator.generateUniqueCode(originalUrl);
        
        // If first attempt fails, use synchronized block with random component
        while (retryCount < Constants.MAX_RETRIES) {
            // Use object lock for specific code to prevent duplicate generation
            Object lock = codeGenerationLocks.computeIfAbsent(code, k -> new Object());
            
            synchronized (lock) {
                try {
                    if (isCodeAvailable(code)) {
                        codeGenerationLocks.remove(code);
                        return code;
                    }
                    
                    // Generate new code with random component
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

    /**
     * Validates the input URL.
     *
     * @param url the URL to validate
     * @throws InvalidUrlException if the URL is invalid
     */
    private void validateUrl(String url) {
        if (!UrlValidator.isValidUrl(url)) {
            throw new InvalidUrlException("Invalid URL format: " + url);
        }
        if (url.length() > MAX_URL_LENGTH) {
            throw new InvalidUrlException("URL exceeds maximum length of " + MAX_URL_LENGTH);
        }
    }

    /**
     * Checks if the generated code is available.
     *
     * @param code the code to check
     * @return true if code is available, false otherwise
     */
    private boolean isCodeAvailable(String code) {
        return urlRepository.findByTinyUrl(code) == null;
    }

    /**
     * Generates a random code using secure random number generator.
     *
     * @return randomly generated code
     */
    private String generateRandomCode() {
        StringBuilder code = new StringBuilder(Constants.CODE_LENGTH);
        for (int i = 0; i < Constants.CODE_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(Constants.BASE62_CHARACTERS.length());
            code.append(Constants.BASE62_CHARACTERS.charAt(randomIndex));
        }
        return code.toString();
    }

    /**
     * Normalizes the URL by removing trailing slashes and converting to lowercase.
     *
     * @param url the URL to normalize
     * @return normalized URL
     */
    private String normalizeUrl(String url) {
//        todo
        return url;
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String urlPattern = "^(https?://)?"              // Optional protocol
                + "(([\\w\\d]([\\w\\d-]*[\\w\\d])*)\\.)+" // Domain name
                + "[a-zA-Z]{2,}"             // TLD
                + "(:\\d{1,5})?"             // Optional port
                + "(/.*)?$";                 // Optional path

        Pattern pattern = Pattern.compile(urlPattern);
        return pattern.matcher(url).matches();
    }

    private void updateUrlStats(Url url) {
        try {
            redisService.incrementClickCount(url.getId());
            // Add more analytics tracking here
        } catch (Exception e) {
            log.warn("Failed to update URL statistics", e);
            // Don't throw exception for analytics failures
        }
    }

    @Transactional
    public Url createNewShortenedUrl(String normalizedUrl) {
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
