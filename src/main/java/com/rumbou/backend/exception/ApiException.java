package com.rumbou.backend.exception;

import org.springframework.http.HttpStatus;

// Raiz de las excepciones propias: cada subclase fija su codigo HTTP y
// GlobalExceptionHandler las atiende a todas con un solo metodo.
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    protected ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
