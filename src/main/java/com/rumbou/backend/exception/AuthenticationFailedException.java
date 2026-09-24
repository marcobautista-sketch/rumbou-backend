package com.rumbou.backend.exception;

import org.springframework.http.HttpStatus;

// 401: no se pudo establecer quien hace la peticion (credenciales, token o firma invalidos).
public abstract class AuthenticationFailedException extends ApiException {

    protected AuthenticationFailedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
