package com.github.admarple.barbeque;

public class SecretException extends RuntimeException {
    public SecretException(String message) {
        super(message);
    }

    public SecretException(String message, Exception cause) {
        super(message, cause);
    }
}
