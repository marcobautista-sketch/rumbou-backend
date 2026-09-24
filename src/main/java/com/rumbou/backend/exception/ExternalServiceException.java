package com.rumbou.backend.exception;

import org.springframework.http.HttpStatus;

// Raiz de los fallos de servicios externos (Gemini, Mercado Pago, correo): 502, no es culpa del usuario.
public class ExternalServiceException extends ApiException {

    public ExternalServiceException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message, cause);
    }
}
