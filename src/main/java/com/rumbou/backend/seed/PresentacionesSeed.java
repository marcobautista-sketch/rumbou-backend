package com.rumbou.backend.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumbou.backend.mapper.PresentacionMapper.Presentacion;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

// Lector de seed/presentaciones.json: la version con formato de cada pregunta, identificada por
// su clave natural (tema, enunciado). Lo usan el seed real y el test de consistencia.
final class PresentacionesSeed {

    static final String ARCHIVO = "seed/presentaciones.json";

    record Ficha(String tema, String enunciado, Presentacion presentacion) {
    }

    private PresentacionesSeed() {
    }

    static List<Ficha> leer() {
        ClassPathResource recurso = new ClassPathResource(ARCHIVO);
        if (!recurso.exists()) {
            return List.of();
        }
        try (InputStream entrada = recurso.getInputStream()) {
            return new ObjectMapper().readValue(entrada, new TypeReference<List<Ficha>>() {
            });
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer el archivo de seed " + ARCHIVO, ex);
        }
    }
}
