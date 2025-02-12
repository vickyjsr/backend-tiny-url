package com.tiny.url.constants;

public class Constants {

    public static final int CODE_LENGTH = 8; // Length of the shortened code
    public static final String BASE62_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    public static final int MAX_RETRIES = 5;
    public static final int INITIAL_CAPACITY = 1000;
    public static final float LOAD_FACTOR = 0.75f;
    public static final int CONCURRENCY_LEVEL = 16;
    
    private Constants() {
        // Private constructor to prevent instantiation
    }
}
