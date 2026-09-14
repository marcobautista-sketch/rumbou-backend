package com.rumbou.backend.contenido.gemini;

import com.rumbou.backend.shared.exception.ExternalServiceException;

public class GeminiException extends ExternalServiceException {

    public GeminiException(String message) {
        super(message);
    }

    public GeminiException(String message, Throwable cause) {
        super(message, cause);
    }
}
