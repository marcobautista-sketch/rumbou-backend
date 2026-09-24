package com.rumbou.backend.exception;

import org.springframework.http.HttpStatus;

// 403: el usuario esta autenticado pero no puede hacer esto (no es el dueno, no tiene el rol).
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
