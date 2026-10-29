package com.rumbou.backend.controller;

import com.rumbou.backend.dto.response.AsistenteResponse;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.security.JwtService;
import com.rumbou.backend.service.AsistenteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AsistenteController.class)
class AsistenteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AsistenteService asistenteService;

    // JwtAuthenticationFilter se instancia igual en el slice: necesita sus dependencias aunque no se ejecute.
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private final Usuario postulante = new Usuario("ana@rumbou.com", "hash", "Ana", Role.USER);

    @Test
    void unaPreguntaEscritaDevuelveLaRespuesta() throws Exception {
        given(asistenteService.responderTexto(eq("¿Que repaso hoy?"), any()))
                .willReturn(new AsistenteResponse("¿Que repaso hoy?", "Empieza por Álgebra."));

        mockMvc.perform(post("/api/v1/asistente/mensajes")
                        .contentType("application/json")
                        .content("{\"mensaje\":\"¿Que repaso hoy?\",\"historial\":[{\"rol\":\"ASISTENTE\",\"texto\":\"Hola\"}]}")
                        .with(user(postulante))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").value("Empieza por Álgebra."));
    }

    @Test
    void unMensajeVacioResponde400() throws Exception {
        mockMvc.perform(post("/api/v1/asistente/mensajes")
                        .contentType("application/json")
                        .content("{\"mensaje\":\"  \"}")
                        .with(user(postulante))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(asistenteService);
    }

    @Test
    void elAudioYElHistorialLleganComoMultipart() throws Exception {
        given(asistenteService.responderVoz(any(), eq("[]")))
                .willReturn(new AsistenteResponse("¿Cuando es mi examen?", "El 15 de febrero."));

        mockMvc.perform(multipart("/api/v1/asistente/voz")
                        .file(new MockMultipartFile("audio", "pregunta.m4a", "audio/mp4", new byte[]{1, 2}))
                        .param("historial", "[]")
                        .with(user(postulante))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcripcion").value("¿Cuando es mi examen?"));
    }

    @Test
    void sinSesionResponde401() throws Exception {
        mockMvc.perform(multipart("/api/v1/asistente/voz")
                        .file(new MockMultipartFile("audio", "pregunta.m4a", "audio/mp4", new byte[]{1}))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(asistenteService);
    }
}
