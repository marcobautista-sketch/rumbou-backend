package com.rumbou.backend.client.gemini;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// No reemplaza la revision humana (aprobada siempre se guarda en false):
// solo evita persistir preguntas obviamente mal formadas. Tiene dos niveles:
// la validacion estructural (gratis, sin IA) y la de "resolver" (Gemini vuelve
// a contestar la pregunta a ciegas y debe dar la misma clave).
@Component
public class GeminiPreguntaValidator {

    private final GeminiClient geminiClient;

    public GeminiPreguntaValidator(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    // Solo forma: no llama a Gemini.
    public Optional<String> validarEstructura(PreguntaGeneradaDto pregunta) {
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

        return Optional.empty();
    }

    // Una pregunta suelta: estructura + una llamada a Gemini para resolverla.
    public Optional<String> validar(PreguntaGeneradaDto pregunta) {
        Optional<String> rechazoEstructural = validarEstructura(pregunta);
        if (rechazoEstructural.isPresent()) {
            return rechazoEstructural;
        }

        int claveResuelta = geminiClient.resolver(pregunta.enunciado(), pregunta.alternativas());
        return compararClave(pregunta, claveResuelta);
    }

    // Un lote completo: estructura de cada una y UNA sola llamada a Gemini para
    // resolver todas las que pasaron la estructura. Devuelve el motivo de
    // rechazo por pregunta, en el mismo orden (Optional.empty() = valida).
    public List<Optional<String>> validarLote(List<PreguntaGeneradaDto> preguntas) {
        List<Optional<String>> rechazos = new ArrayList<>();
        List<Integer> posicionesAResolver = new ArrayList<>();
        List<PreguntaGeneradaDto> aResolver = new ArrayList<>();

        for (int i = 0; i < preguntas.size(); i++) {
            Optional<String> rechazo = validarEstructura(preguntas.get(i));
            rechazos.add(rechazo);
            if (rechazo.isEmpty()) {
                posicionesAResolver.add(i);
                aResolver.add(preguntas.get(i));
            }
        }

        if (aResolver.isEmpty()) {
            return rechazos;
        }

        List<Integer> clavesResueltas = geminiClient.resolverLote(aResolver);
        for (int j = 0; j < aResolver.size(); j++) {
            rechazos.set(posicionesAResolver.get(j), compararClave(aResolver.get(j), clavesResueltas.get(j)));
        }
        return rechazos;
    }

    private Optional<String> compararClave(PreguntaGeneradaDto pregunta, int claveResuelta) {
        if (claveResuelta != pregunta.claveCorrecta()) {
            return Optional.of("Gemini no reprodujo la misma clave al resolver la pregunta "
                    + "(genero " + pregunta.claveCorrecta() + ", resolvio " + claveResuelta + ")");
        }
        return Optional.empty();
    }
}
