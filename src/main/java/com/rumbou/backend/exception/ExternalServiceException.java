package com.rumbou.backend.exception;

// Raiz de los fallos de servicios externos (Gemini, Mercado Pago, correo): 502, no es culpa del usuario.
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
