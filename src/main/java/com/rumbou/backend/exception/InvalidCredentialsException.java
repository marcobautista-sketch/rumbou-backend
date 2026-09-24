package com.rumbou.backend.exception;


public class InvalidCredentialsException extends AuthenticationFailedException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
