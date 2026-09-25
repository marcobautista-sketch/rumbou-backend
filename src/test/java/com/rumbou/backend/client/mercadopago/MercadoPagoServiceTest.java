package com.rumbou.backend.client.mercadopago;

import com.mercadopago.client.preapproval.PreapprovalCreateRequest;
import com.rumbou.backend.exception.ExternalServiceException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MercadoPagoServiceTest {

    private static final String BACK_URL = "https://rumbou.app/suscripcion";

    @Test
    void laSolicitudLlevaElBackUrlQueMercadoPagoExige() {
        MercadoPagoService service = new MercadoPagoService("token", BACK_URL, "");

        PreapprovalCreateRequest solicitud = service.construirSolicitud("suscripcion-1", "ana@rumbou.com");

        assertThat(solicitud.getBackUrl()).isEqualTo(BACK_URL);
        assertThat(solicitud.getExternalReference()).isEqualTo("suscripcion-1");
        assertThat(solicitud.getAutoRecurring().getTransactionAmount()).isEqualByComparingTo(new BigDecimal("39"));
        assertThat(solicitud.getAutoRecurring().getCurrencyId()).isEqualTo("PEN");
    }

    @Test
    void sinCompradorDePruebaCobraAlCorreoDelUsuario() {
        MercadoPagoService service = new MercadoPagoService("token", BACK_URL, "");

        PreapprovalCreateRequest solicitud = service.construirSolicitud("suscripcion-1", "ana@rumbou.com");

        assertThat(solicitud.getPayerEmail()).isEqualTo("ana@rumbou.com");
    }

    @Test
    void conCompradorDePruebaLaSolicitudSaleASuNombre() {
        MercadoPagoService service = new MercadoPagoService("token", BACK_URL, "test_user_1@testuser.com");

        PreapprovalCreateRequest solicitud = service.construirSolicitud("suscripcion-1", "ana@rumbou.com");

        assertThat(solicitud.getPayerEmail()).isEqualTo("test_user_1@testuser.com");
    }

    @Test
    void sinTokenRespondeComoServicioExternoNoConfigurado() {
        MercadoPagoService service = new MercadoPagoService("", BACK_URL, "");

        assertThatThrownBy(() -> service.crearPreaprobacion("suscripcion-1", "ana@rumbou.com"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("MP_ACCESS_TOKEN");
    }
}
