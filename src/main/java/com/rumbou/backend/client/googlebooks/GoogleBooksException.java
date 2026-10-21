package com.rumbou.backend.client.googlebooks;

import com.rumbou.backend.exception.ExternalServiceException;

public class GoogleBooksException extends ExternalServiceException {

    public GoogleBooksException(String message) {
        super(message);
    }

    public GoogleBooksException(String message, Throwable cause) {
        super(message, cause);
    }
}
