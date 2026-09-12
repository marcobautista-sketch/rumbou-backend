package com.rumbou.backend.contenido.gemini;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

// No reemplaza la revision humana (aprobada siempre se guarda en false):
// solo evita persistir preguntas obviamente mal formadas.
@Component
public class GeminiPreguntaValidator {

    private final GeminiClient geminiClient;

    public GeminiPreguntaValidator(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public Optional<String> validar(PreguntaGeneradaDto pregunta) {
        if (pregunta.enunciado() == null || pregunta.enunciado().isBlank()) {
            return Optional.of("enunciado vacio");
        }

        List<String> alternativas = pregunta.alternativas();
        if (alternativas == null || alternativas.size() != 5) {
            return Optional.of("no tiene exactamente 5 alternativas");
        }
        if (alternativas.stream().anyMatch(a -> a == null || a.isBlank())) {
            return Optional.of("hay alternativas vacias");
        }

        if (pregunta.claveCorrecta() < 0 || pregunta.claveCorrecta() > 4) {
            return Optional.of("claveCorrecta fuera de rango (0-4)");
        }

        if (pregunta.explicacion() == null || pregunta.explicacion().isBlank()) {
            return Optional.of("explicacion vacia");
        }

        int claveResuelta = geminiClient.resolver(pregunta.enunciado(), alternativas);
        if (claveResuelta != pregunta.claveCorrecta()) {
            return Optional.of("Gemini no reprodujo la misma clave al resolver la pregunta "
                    + "(genero " + pregunta.claveCorrecta() + ", resolvio " + claveResuelta + ")");
        }

        return Optional.empty();
    }
}
