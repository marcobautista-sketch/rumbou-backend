package com.rumbou.backend.client.mercadopago;

import com.mercadopago.MercadoPagoConfig;
import com.rumbou.backend.exception.ExternalServiceException;
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

    // El token vive solo en la variable de entorno MP_ACCESS_TOKEN.
    private final String accessToken;

    public MercadoPagoService(@Value("${mercadopago.access-token}") String accessToken) {
        this.accessToken = accessToken;
    }

    public record ResultadoPreaprobacion(String preapprovalId, String externalReference, String initPoint) {
    }

    public ResultadoPreaprobacion crearPreaprobacion(String externalReference, String payerEmail) {
        if (accessToken.isBlank()) {
            throw new ExternalServiceException("Falta configurar MP_ACCESS_TOKEN");
        }
        MercadoPagoConfig.setAccessToken(accessToken);

        PreApprovalAutoRecurringCreateRequest autoRecurring = PreApprovalAutoRecurringCreateRequest.builder()
                .frequency(1)
                .frequencyType("months")
                .transactionAmount(MONTO_PRO_SOLES)
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
                    preapproval.getInitPoint()
            );
        } catch (MPException | MPApiException e) {
            throw new ExternalServiceException("No se pudo crear la preaprobacion en Mercado Pago: " + e.getMessage(), e);
        }
    }
}