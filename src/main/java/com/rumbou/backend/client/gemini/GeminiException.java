package com.rumbou.backend.client.gemini;

import com.rumbou.backend.exception.ExternalServiceException;

public class GeminiException extends ExternalServiceException {

    public GeminiException(String message) {
        super(message);
    }

    public GeminiException(String message, Throwable cause) {
        super(message, cause);
    }
}
