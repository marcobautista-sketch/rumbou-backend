package com.rumbou.backend.suscripcion;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preapproval.PreApprovalAutoRecurringCreateRequest;
import com.mercadopago.client.preapproval.PreapprovalClient;
import com.mercadopago.client.preapproval.PreapprovalCreateRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preapproval.Preapproval;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class MercadoPagoService {

    private static final BigDecimal MONTO_PRO_SOLES = new BigDecimal("39");
    private static final String MONEDA_PERU = "PEN";

    // La llave de acceso vive SOLO en la variable de entorno MP_ACCESS_TOKEN (regla 9).
    @Value("${MP_ACCESS_TOKEN:}")
    private String accessToken;

    public record ResultadoPreaprobacion(String preapprovalId, String externalReference, String initPoint) {
    }

    public ResultadoPreaprobacion crearPreaprobacion(String externalReference, String payerEmail) {
        if (accessToken.isBlank()) {
            throw new IllegalStateException("Falta configurar MP_ACCESS_TOKEN");
        }
        MercadoPagoConfig.setAccessToken(accessToken);

        PreApprovalAutoRecurringCreateRequest autoRecurring = PreApprovalAutoRecurringCreateRequest.builder()
                .frequency(1)                    // cada 1...
                .frequencyType("months")         // ...mes
                .transactionAmount(MONTO_PRO_SOLES)   // S/39 (regla de negocio, no del examen)
                .currencyId(MONEDA_PERU)
                .build();

        PreapprovalCreateRequest request = PreapprovalCreateRequest.builder()
                .reason("Plan PRO RumboU")
                .externalReference(externalReference)
                .payerEmail(payerEmail)
                .autoRecurring(autoRecurring)
                .build();

        try {
            Preapproval preapproval = new PreapprovalClient().create(request);
            return new ResultadoPreaprobacion(
                    preapproval.getId(),
                    externalReference,
                    preapproval.getInitPoint()  // el link donde el usuario paga
            );
        } catch (MPException | MPApiException e) {
            throw new IllegalStateException("No se pudo crear la preaprobacion en Mercado Pago: " + e.getMessage(), e);
        }
    }
}