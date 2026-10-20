package com.rumbou.backend.dto.response;

import java.util.List;

// cantidadPreguntas: las que trae el examen real (y un simulacro POR_TEMA).
// subtemas: los del temario oficial de la universidad del area.
public record TemaResponse(
        Long id,
        String nombre,
        int cantidadPreguntas,
        List<String> subtemas
) {
}
