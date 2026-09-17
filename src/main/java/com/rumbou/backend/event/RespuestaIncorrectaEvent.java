package com.rumbou.backend.event;

// Asincrono (el listener futuro va con @Async + AFTER_COMMIT). Lo escucha
// el modulo de Contenido para llamar al tutor de IA y generar/cachear la
// explicacion de esta pregunta puntual.
public record RespuestaIncorrectaEvent(
        Long respuestaUsuarioId,
        Long preguntaId,
        Long usuarioId
) {
}
