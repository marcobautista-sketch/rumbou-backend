package com.rumbou.backend.client.gemini;

import com.rumbou.backend.entity.VeredictoSolucion;

public record RevisionSolucionDto(
        VeredictoSolucion veredicto,
        String retroalimentacion
) {
}
