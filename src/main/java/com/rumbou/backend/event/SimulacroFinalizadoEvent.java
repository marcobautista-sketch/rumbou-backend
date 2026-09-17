package com.rumbou.backend.event;

// Lo publica SimulacroService al calificar; lo escucha GamificacionListener (sincrono, misma transaccion).
public record SimulacroFinalizadoEvent(
        Long simulacroId,
        Long usuarioId,
        Long areaId,
        double puntajeObtenido,
        double psp
) {
}
