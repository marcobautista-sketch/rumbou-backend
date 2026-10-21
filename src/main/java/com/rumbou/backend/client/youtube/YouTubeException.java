package com.rumbou.backend.client.youtube;

import com.rumbou.backend.exception.ExternalServiceException;

public class YouTubeException extends ExternalServiceException {

    public YouTubeException(String message) {
        super(message);
    }

    public YouTubeException(String message, Throwable cause) {
        super(message, cause);
    }
}
