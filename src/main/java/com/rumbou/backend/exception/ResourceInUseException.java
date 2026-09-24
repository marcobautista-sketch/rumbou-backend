package com.rumbou.backend.exception;

// El recurso no se puede borrar porque otros registros dependen de el.
public class ResourceInUseException extends ConflictException {

    public ResourceInUseException(String message) {
        super(message);
    }
}
