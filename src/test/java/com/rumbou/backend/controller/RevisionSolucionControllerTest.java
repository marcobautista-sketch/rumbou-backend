package com.rumbou.backend.controller;

import com.rumbou.backend.dto.response.RevisionSolucionResponse;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.entity.VeredictoSolucion;
import com.rumbou.backend.security.JwtService;
import com.rumbou.backend.service.RevisionSolucionService;
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

@WebMvcTest(RevisionSolucionController.class)
class RevisionSolucionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RevisionSolucionService revisionSolucionService;

    // JwtAuthenticationFilter se instancia igual en el slice: necesita sus dependencias aunque no se ejecute.
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private final Usuario postulante = new Usuario("postulante@rumbou.com", "hash", "Ana", Role.USER);

    @Test
    void laFotoLlegaComoMultipartYDevuelveElVeredicto() throws Exception {
        given(revisionSolucionService.revisar(eq(1L), any()))
                .willReturn(new RevisionSolucionResponse(1L, VeredictoSolucion.INCORRECTA, "Revisa el paso 2"));

        mockMvc.perform(multipart("/api/v1/preguntas/1/revisar-solucion")
                        .file(new MockMultipartFile("imagen", "solucion.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .with(user(postulante))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.veredicto").value("INCORRECTA"))
                .andExpect(jsonPath("$.retroalimentacion").value("Revisa el paso 2"));
    }

    @Test
    void sinLaParteImagenResponde400() throws Exception {
        mockMvc.perform(multipart("/api/v1/preguntas/1/revisar-solucion")
                        .file(new MockMultipartFile("foto", "solucion.jpg", "image/jpeg", new byte[]{1}))
                        .with(user(postulante))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el archivo requerido: imagen"));

        verifyNoInteractions(revisionSolucionService);
    }

    @Test
    void unCuerpoJsonResponde415IndicandoQueEsperaMultipart() throws Exception {
        mockMvc.perform(post("/api/v1/preguntas/1/revisar-solucion")
                        .contentType("application/json")
                        .content("{}")
                        .with(user(postulante))
                        .with(csrf()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("El cuerpo debe enviarse como multipart/form-data"));
    }

    @Test
    void sinSesionNoSePuedeEnviarUnaFoto() throws Exception {
        mockMvc.perform(multipart("/api/v1/preguntas/1/revisar-solucion")
                        .file(new MockMultipartFile("imagen", "solucion.jpg", "image/jpeg", new byte[]{1}))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(revisionSolucionService);
    }
}
