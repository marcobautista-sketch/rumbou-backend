package com.rumbou.backend.exception;

// Raiz para fallos de servicios externos (Gemini, y a futuro Mercado Pago,
// proveedor de correo, etc). Se mapea a 502: el error no es culpa del usuario.
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
