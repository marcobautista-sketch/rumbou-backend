package com.rumbou.backend.dto.response;

import com.rumbou.backend.entity.VeredictoSolucion;

public record RevisionSolucionResponse(
        Long preguntaId,
        VeredictoSolucion veredicto,
        String retroalimentacion
) {
}
