package com.rumbou.backend.event;

// Lo publica SimulacroService al calificar; lo escuchan GamificacionListener y ProgresoListener (sincronos, misma transaccion).
public record SimulacroFinalizadoEvent(
        Long simulacroId,
        Long usuarioId,
        Long areaId,
        double puntajeObtenido,
        double psp
) {
}
