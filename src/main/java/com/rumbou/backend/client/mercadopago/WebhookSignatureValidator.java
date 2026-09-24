package com.rumbou.backend.client.mercadopago;

import com.rumbou.backend.exception.InvalidWebhookSignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

// Verifica el header x-signature de Mercado Pago ("ts=...,v1=..."): v1 es el
// HMAC-SHA256, con MP_WEBHOOK_SECRET, del manifiesto "id:<id>;request-id:<x-request-id>;ts:<ts>;".
// Sin esto cualquiera podria llamar al webhook y activarse el plan PRO.
@Component
public class WebhookSignatureValidator {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureValidator.class);
    private static final String ALGORITMO = "HmacSHA256";

    private final String secret;

    public WebhookSignatureValidator(@Value("${mercadopago.webhook-secret}") String secret) {
        this.secret = secret;
        if (secret.isBlank()) {
            log.warn("MP_WEBHOOK_SECRET no esta configurado: el webhook de Mercado Pago no valida firmas");
        }
    }

    public void validar(String xSignature, String xRequestId, String dataId) {
        if (secret.isBlank()) {
            return;
        }
        if (xSignature == null || xSignature.isBlank()) {
            throw new InvalidWebhookSignatureException("Falta la firma x-signature de Mercado Pago");
        }

        String ts = extraerParte(xSignature, "ts");
        String v1 = extraerParte(xSignature, "v1");
        String manifiesto = "id:" + dataId + ";request-id:" + (xRequestId == null ? "" : xRequestId) + ";ts:" + ts + ";";

        byte[] esperada = hmac(manifiesto).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(esperada, v1.getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidWebhookSignatureException("La firma de la notificacion no es valida");
        }
    }

    private String extraerParte(String xSignature, String clave) {
        for (String parte : xSignature.split(",")) {
            String[] claveValor = parte.trim().split("=", 2);
            if (claveValor.length == 2 && claveValor[0].equals(clave)) {
                return claveValor[1];
            }
        }
        throw new InvalidWebhookSignatureException("La firma x-signature no trae el campo " + clave);
    }

    private String hmac(String mensaje) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITMO));
            return HexFormat.of().formatHex(mac.doFinal(mensaje.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular la firma HMAC", e);
        }
    }
}
