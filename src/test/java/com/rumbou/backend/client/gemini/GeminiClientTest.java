package com.rumbou.backend.client.gemini;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeminiClientTest {

    @Test
    void conLaIaApagadaNoSeLlamaAlServicioExterno() {
        GeminiClient client = new GeminiClient("clave", "modelo", false, new ObjectMapper());

        assertThatThrownBy(() -> client.explicar("Enunciado", List.of("Uno", "Dos"), 0))
                .isInstanceOf(GeminiException.class)
                .hasMessageContaining("desactivada");
    }
}
