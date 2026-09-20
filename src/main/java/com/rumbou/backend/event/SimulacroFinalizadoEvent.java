package com.rumbou.backend.event;

import com.rumbou.backend.entity.TipoSimulacro;

// Lo publica SimulacroService al calificar; lo escuchan GamificacionListener y
// ProgresoListener (sincronos, misma transaccion). Lleva el tipo porque un
// simulacro POR_TEMA no debe mover el indice de preparacion: su PSP sale de un
// solo tema y no representa el examen completo.
public record SimulacroFinalizadoEvent(
        Long simulacroId,
        Long usuarioId,
        Long areaId,
        TipoSimulacro tipo,
        double puntajeObtenido,
        double psp
) {
}
