package com.rumbou.backend.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumbou.backend.dto.response.PasoResolucionResponse;
import com.rumbou.backend.dto.response.PresentacionResponse;
import com.rumbou.backend.entity.Pregunta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

// Lee la version con formato que se guarda como JSON en Pregunta.presentacion y la parte en dos:
// el problema (se puede mostrar siempre) y los pasos (solo cuando ya se revela la explicacion).
public final class PresentacionMapper {

    private static final Logger log = LoggerFactory.getLogger(PresentacionMapper.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    // Formato guardado en la columna y en seed/presentaciones.json.
    public record Presentacion(
            String enunciado,
            List<String> alternativas,
            JsonNode figura,
            List<PasoResolucionResponse> pasos
    ) {
    }

    private PresentacionMapper() {
    }

    public static Presentacion leer(String json) throws JsonProcessingException {
        return JSON.readValue(json, Presentacion.class);
    }

    public static String escribir(Presentacion presentacion) {
        try {
            return JSON.writeValueAsString(presentacion);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo convertir la presentacion a JSON", ex);
        }
    }

    public static PresentacionResponse problema(Pregunta pregunta) {
        Presentacion presentacion = leerGuardada(pregunta);
        if (presentacion == null) {
            return null;
        }
        JsonNode figura = presentacion.figura();
        return new PresentacionResponse(
                presentacion.enunciado(),
                presentacion.alternativas(),
                figura == null || figura.isNull() ? null : figura);
    }

    public static List<PasoResolucionResponse> pasos(Pregunta pregunta) {
        Presentacion presentacion = leerGuardada(pregunta);
        if (presentacion == null || presentacion.pasos() == null || presentacion.pasos().isEmpty()) {
            return null;
        }
        return presentacion.pasos();
    }

    // Un JSON danado no debe tumbar la pregunta: se avisa y se muestra el texto plano.
    private static Presentacion leerGuardada(Pregunta pregunta) {
        String json = pregunta.getPresentacion();
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return leer(json);
        } catch (JsonProcessingException ex) {
            log.warn("Presentacion invalida en la pregunta {}: {}", pregunta.getId(), ex.getOriginalMessage());
            return null;
        }
    }
}
