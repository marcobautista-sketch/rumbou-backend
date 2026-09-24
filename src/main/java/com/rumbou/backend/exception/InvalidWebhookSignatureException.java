package com.rumbou.backend.exception;

// La notificacion no trae una firma valida de Mercado Pago: no se procesa.
public class InvalidWebhookSignatureException extends AuthenticationFailedException {

    public InvalidWebhookSignatureException(String message) {
        super(message);
    }
}
