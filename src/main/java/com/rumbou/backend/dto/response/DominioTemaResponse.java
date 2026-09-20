package com.rumbou.backend.dto.response;

// porcentajeAciertos = correctas / (correctas + incorrectas + enBlanco): dejar una
// pregunta en blanco tambien cuenta como falta de dominio.
public record DominioTemaResponse(
        Long temaId,
        String tema,
        long correctas,
        long incorrectas,
        long enBlanco,
        long total,
        double porcentajeAciertos
) {
}
