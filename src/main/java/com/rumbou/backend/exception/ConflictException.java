package com.rumbou.backend.exception;

import org.springframework.http.HttpStatus;

// 409: la operacion choca con el estado actual de los datos.
public abstract class ConflictException extends ApiException {

    protected ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
