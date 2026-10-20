package com.rumbou.backend.event;

// Lo publica DesafioService al responder el desafio del dia; lo escucha GamificacionListener
// (sincrono, misma transaccion) para sumar el XP y mantener la racha.
public record DesafioCompletadoEvent(
        Long usuarioId,
        int xp
) {
}
