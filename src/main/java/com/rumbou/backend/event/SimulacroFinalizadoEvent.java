package com.rumbou.backend.event;

// Evento sincrono y transaccional: se publica al terminar de calificar un
// simulacro. Lo escuchan Progreso (recalcula PSP/IP/dominio por tema) y
// Gamificacion (actualiza XP y racha). Ninguno de los dos existe todavia
// en el proyecto, y no hace falta: publicar un evento sin listeners no
// rompe nada, Spring simplemente no hace nada con el.
public record SimulacroFinalizadoEvent(
        Long simulacroId,
        Long usuarioId,
        Long areaId,
        double puntajeObtenido,
        double psp
) {
}
