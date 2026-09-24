package com.rumbou.backend.client.mercadopago;

import com.rumbou.backend.exception.InvalidWebhookSignatureException;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookSignatureValidatorTest {

    private static final String SECRETO = "secreto-webhook";

    private final WebhookSignatureValidator validator = new WebhookSignatureValidator(SECRETO);

    @Test
    void aceptaUnaFirmaCalculadaConElSecreto() throws Exception {
        String firma = "ts=1704908010,v1=" + hmac("id:123;request-id:req-1;ts:1704908010;");

        assertThatCode(() -> validator.validar(firma, "req-1", "123")).doesNotThrowAnyException();
    }

    @Test
    void rechazaUnaFirmaAlterada() throws Exception {
        String firma = "ts=1704908010,v1=" + hmac("id:999;request-id:req-1;ts:1704908010;");

        assertThatThrownBy(() -> validator.validar(firma, "req-1", "123"))
                .isInstanceOf(InvalidWebhookSignatureException.class);
    }

    @Test
    void rechazaUnaNotificacionSinFirma() {
        assertThatThrownBy(() -> validator.validar(null, "req-1", "123"))
                .isInstanceOf(InvalidWebhookSignatureException.class);
    }

    @Test
    void sinSecretoConfiguradoNoValida() {
        WebhookSignatureValidator sinSecreto = new WebhookSignatureValidator("");

        assertThatCode(() -> sinSecreto.validar(null, null, "123")).doesNotThrowAnyException();
    }

    private String hmac(String mensaje) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(mensaje.getBytes(StandardCharsets.UTF_8)));
    }
}
