package com.tiny.url.helpers;

import com.tiny.url.constants.Constants;
import com.tiny.url.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Component
public class CodeGenerator {

    private static final int MAX_RETRIES = 3;
    private final SecureRandom secureRandom;
    private final MessageDigest messageDigest;
    private final UrlRepository urlRepository;

    @Autowired
    public CodeGenerator(UrlRepository urlRepository) throws NoSuchAlgorithmException {
        this.messageDigest = MessageDigest.getInstance("SHA-256");
        this.secureRandom = new SecureRandom();
        this.urlRepository = urlRepository;
    }

    public String generateUniqueCode(String originalUrl) {
        // First try deterministic approach
        String code = generateDeterministicCode(originalUrl);
        if (isCodeAvailable(code)) {
            return code;
        }

        // Fall back to random generation with collision handling
        return generateRandomCodeWithRetry(originalUrl);
    }

    private String generateDeterministicCode(String originalUrl) {
        messageDigest.reset();
        byte[] digest = messageDigest.digest(originalUrl.getBytes());
        return encodeBase62(digest).substring(0, Constants.CODE_LENGTH);
    }

    private String generateRandomCodeWithRetry(String originalUrl) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            String code = generateRandomCode();
            if (isCodeAvailable(code)) {
                return code;
            }
        }
        throw new RuntimeException("Failed to generate unique code after " + MAX_RETRIES + " attempts");
    }

    private String generateRandomCode() {
        byte[] randomBytes = new byte[Constants.CODE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return encodeBase62(randomBytes).substring(0, Constants.CODE_LENGTH);
    }

    private String encodeBase62(byte[] input) {
        // Implement proper Base62 encoding
        StringBuilder result = new StringBuilder();
        BigInteger number = new BigInteger(1, input);
        while (number.compareTo(BigInteger.ZERO) > 0) {
            int remainder = number.mod(BigInteger.valueOf(62)).intValue();
            result.insert(0, Constants.BASE62_CHARACTERS.charAt(remainder));
            number = number.divide(BigInteger.valueOf(62));
        }
        return result.toString();
    }

    private boolean isCodeAvailable(String code) {
        return urlRepository.findByTinyUrl(code) == null;
    }
}
