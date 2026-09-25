package com.rumbou.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumbou.backend.dto.request.CrearObjetivoRequest;
import com.rumbou.backend.dto.response.EstadoPreparacion;
import com.rumbou.backend.dto.response.ObjetivoResponse;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.ForbiddenException;
import com.rumbou.backend.security.JwtService;
import com.rumbou.backend.service.ObjetivoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ObjetivoController.class)
class ObjetivoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ObjetivoService objetivoService;

    // JwtAuthenticationFilter se instancia igual en el slice: necesita sus dependencias aunque no se ejecute.
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private Usuario postulante() {
        return new Usuario("postulante@rumbou.com", "hash", "Ana", Role.USER);
    }

    private ObjetivoResponse objetivo() {
        return new ObjetivoResponse(1L, 10L, "UNI", 4L, "GENERAL", "Ingenieria de Sistemas", "2026-II",
                1209, 1330.0, 1.1, EstadoPreparacion.HOLGADO, "Zona de ingreso holgada", LocalDateTime.now());
    }

    @Test
    void crearUnObjetivoDevuelve201YElPanelDeEsaCarrera() throws Exception {
        given(objetivoService.crearObjetivo(10L)).willReturn(objetivo());

        mockMvc.perform(post("/api/v1/objetivos")
                        .with(user(postulante()))
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CrearObjetivoRequest(10L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carrera").value("Ingenieria de Sistemas"))
                .andExpect(jsonPath("$.estado").value("HOLGADO"))
                .andExpect(jsonPath("$.estadoDescripcion").value("Zona de ingreso holgada"));
    }

    @Test
    void rechazaCrearUnObjetivoSinOfertaAcademica() throws Exception {
        mockMvc.perform(post("/api/v1/objetivos")
                        .with(user(postulante()))
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CrearObjetivoRequest(null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alSuperarElLimiteDelPlanResponde403() throws Exception {
        willThrow(new ForbiddenException("El plan gratuito permite 1 objetivo activo"))
                .given(objetivoService).crearObjetivo(10L);

        mockMvc.perform(post("/api/v1/objetivos")
                        .with(user(postulante()))
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CrearObjetivoRequest(10L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("El plan gratuito permite 1 objetivo activo"));
    }

    @Test
    void listarDevuelveLosObjetivosActivosDelUsuario() throws Exception {
        given(objetivoService.listarObjetivos()).willReturn(List.of(objetivo()));

        mockMvc.perform(get("/api/v1/objetivos").with(user(postulante())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].universidad").value("UNI"))
                .andExpect(jsonPath("$[0].areaId").value(4))
                .andExpect(jsonPath("$[0].indicePreparacion").value(1.1));
    }

    @Test
    void desactivarUnObjetivoDevuelve204() throws Exception {
        mockMvc.perform(delete("/api/v1/objetivos/1")
                        .with(user(postulante()))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(objetivoService).desactivarObjetivo(1L);
    }
}
