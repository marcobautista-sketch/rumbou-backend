package com.rumbou.backend.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

// Version con formato del problema: enunciado y alternativas con LaTeX ($...$) y una figura
// opcional descrita en coordenadas. No incluye la resolucion, asi que se puede mostrar siempre.
// Un campo null significa "usar el texto plano de la pregunta".
public record PresentacionResponse(
        String enunciado,
        List<String> alternativas,
        JsonNode figura
) {
}
