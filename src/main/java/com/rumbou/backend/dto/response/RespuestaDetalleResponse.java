package com.rumbou.backend.dto.response;

import java.util.List;

// claveCorrecta, esCorrecta, puntajeAportado y explicacion son null mientras el
// simulacro sigue en curso: solo se revelan despues de finalizarlo.
public record RespuestaDetalleResponse(
        Long respuestaUsuarioId,
        Long preguntaId,
        String enunciado,
        List<String> alternativas,
        Integer alternativaMarcada,
        Integer claveCorrecta,
        Boolean esCorrecta,
        Double puntajeAportado,
        String explicacion
) {
}
