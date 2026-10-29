package com.rumbou.backend.client.gemini;

public record RespuestaAsistenteDto(
        String transcripcion,
        String respuesta
) {
}
