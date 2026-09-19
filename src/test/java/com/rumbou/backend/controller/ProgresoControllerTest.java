package com.rumbou.backend.controller;

import com.rumbou.backend.dto.response.DominioTemaResponse;
import com.rumbou.backend.dto.response.HistorialPspResponse;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.TipoSimulacro;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.UnauthorizedException;
import com.rumbou.backend.security.JwtService;
import com.rumbou.backend.service.ProgresoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgresoController.class)
class ProgresoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProgresoService progresoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private Usuario postulante() {
        return new Usuario("postulante@rumbou.com", "hash", "Ana", Role.USER);
    }

    @Test
    void elDominioPorTemaDevuelveLosConteosYElPorcentaje() throws Exception {
        given(progresoService.dominioPorTema(any()))
                .willReturn(List.of(new DominioTemaResponse(1L, "Fisica", 3, 4, 3, 10, 30.0)));

        mockMvc.perform(get("/api/v1/progreso/dominio-temas").with(user(postulante())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tema").value("Fisica"))
                .andExpect(jsonPath("$[0].enBlanco").value(3))
                .andExpect(jsonPath("$[0].porcentajeAciertos").value(30.0));
    }

    @Test
    void unUsuarioGratuitoRecibe403EnElDominioPorTema() throws Exception {
        willThrow(new UnauthorizedException("El dominio por tema es exclusivo del plan PRO"))
                .given(progresoService).dominioPorTema(any());

        mockMvc.perform(get("/api/v1/progreso/dominio-temas").with(user(postulante())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("El dominio por tema es exclusivo del plan PRO"));
    }

    @Test
    void elHistorialDevuelveElPspDeCadaSimulacro() throws Exception {
        given(progresoService.historialPsp(any(), eq(2L))).willReturn(List.of(
                new HistorialPspResponse(1L, TipoSimulacro.COMPLETO, LocalDateTime.now(), 500.0, 900.0),
                new HistorialPspResponse(2L, TipoSimulacro.COMPLETO, LocalDateTime.now(), 620.0, 1100.0)));

        mockMvc.perform(get("/api/v1/progreso/historial?areaId=2").with(user(postulante())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].psp").value(900.0))
                .andExpect(jsonPath("$[1].psp").value(1100.0));
    }

    @Test
    void elHistorialExigeElAreaId() throws Exception {
        mockMvc.perform(get("/api/v1/progreso/historial").with(user(postulante())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el parametro requerido: areaId"));
    }

    @Test
    void elHistorialRechazaUnAreaIdQueNoEsNumero() throws Exception {
        mockMvc.perform(get("/api/v1/progreso/historial?areaId=matematica").with(user(postulante())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parametro areaId tiene un valor invalido"));
    }
}
