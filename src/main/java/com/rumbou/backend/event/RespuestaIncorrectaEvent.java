package com.rumbou.backend.event;

// Lo publica SimulacroService por cada respuesta incorrecta; lo escucha TutorIaExplicacionListener (asincrono).
public record RespuestaIncorrectaEvent(
        Long respuestaUsuarioId,
        Long preguntaId,
        Long usuarioId
) {
}
