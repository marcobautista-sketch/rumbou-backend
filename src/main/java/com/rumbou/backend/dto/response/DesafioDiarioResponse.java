package com.rumbou.backend.dto.response;

import java.time.LocalDate;
import java.util.List;

// claveCorrecta, esCorrecta, explicacion y xpGanado son null hasta que se responde.
public record DesafioDiarioResponse(
        LocalDate fecha,
        Long preguntaId,
        Long temaId,
        String tema,
        String enunciado,
        List<String> alternativas,
        boolean respondido,
        Integer alternativaMarcada,
        Boolean esCorrecta,
        Integer claveCorrecta,
        String explicacion,
        Integer xpGanado,
        int xpPorAcierto,
        long desafiosDeLaSemana,
        int metaSemanal
) {
}
