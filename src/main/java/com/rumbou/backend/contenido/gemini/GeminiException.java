package com.rumbou.backend.contenido.gemini;

// Errores al llamar a la API de Gemini (red, timeout, respuesta con codigo distinto de 200).
// No extiende las excepciones de shared/exception porque esto no es un error de la API REST
// del proyecto: ocurre en un script fuera del runtime normal (ver GeneradorPreguntasRunner).
public class GeminiException extends RuntimeException {

    public GeminiException(String message) {
        super(message);
    }

    public GeminiException(String message, Throwable cause) {
        super(message, cause);
    }
}
