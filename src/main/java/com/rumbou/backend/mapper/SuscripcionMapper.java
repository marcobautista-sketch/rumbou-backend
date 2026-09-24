package com.rumbou.backend.mapper;

import com.rumbou.backend.dto.response.SuscripcionResponse;
import com.rumbou.backend.entity.Suscripcion;

public final class SuscripcionMapper {

    private SuscripcionMapper() {
    }

    // linkPago solo existe al crearla: es el init_point de Mercado Pago.
    public static SuscripcionResponse toResponse(Suscripcion suscripcion, String linkPago) {
        return new SuscripcionResponse(
                suscripcion.getId(),
                suscripcion.getPlan().name(),
                suscripcion.getEstado().name(),
                suscripcion.getFechaInicio(),
                suscripcion.getFechaFin(),
                linkPago);
    }
}
