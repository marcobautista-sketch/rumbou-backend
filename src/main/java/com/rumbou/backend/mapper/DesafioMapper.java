package com.rumbou.backend.mapper;

import com.rumbou.backend.dto.response.DesafioDiarioResponse;
import com.rumbou.backend.entity.DesafioDiario;
import com.rumbou.backend.entity.Pregunta;

public final class DesafioMapper {

    private DesafioMapper() {
    }

    // La clave y la explicacion solo se revelan despues de responder.
    public static DesafioDiarioResponse toResponse(DesafioDiario desafio, long desafiosDeLaSemana,
                                                   int xpPorAcierto, int metaSemanal) {
        Pregunta pregunta = desafio.getPregunta();
        boolean respondido = desafio.estaRespondido();
        return new DesafioDiarioResponse(
                desafio.getFecha(),
                pregunta.getId(),
                pregunta.getTema().getId(),
                pregunta.getTema().getNombre(),
                pregunta.getEnunciado(),
                pregunta.getAlternativas(),
                respondido,
                desafio.getAlternativaMarcada(),
                desafio.getEsCorrecta(),
                respondido ? pregunta.getClaveCorrecta() : null,
                respondido ? pregunta.getExplicacion() : null,
                desafio.getXpGanado(),
                xpPorAcierto,
                desafiosDeLaSemana,
                metaSemanal);
    }
}
