package com.rumbou.backend.exception;


public class InvalidTokenException extends AuthenticationFailedException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
