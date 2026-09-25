package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.SuscripcionResponse;
import com.rumbou.backend.event.PagoAprobadoEvent;

public interface SuscripcionService {

    void activar(PagoAprobadoEvent evento);

    SuscripcionResponse crear();

    SuscripcionResponse obtenerActual();
}
