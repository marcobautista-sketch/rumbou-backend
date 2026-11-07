package com.rumbou.backend.controller;

import com.rumbou.backend.dto.request.ParticipacionRankingRequest;
import com.rumbou.backend.dto.response.RankingEntradaResponse;
import com.rumbou.backend.dto.response.RankingPaginaResponse;
import com.rumbou.backend.dto.response.RankingResponse;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.DuplicateResourceException;
import com.rumbou.backend.security.JwtService;
import com.rumbou.backend.service.RankingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RankingController.class)
@Import(RankingControllerTest.SeguridadTestConfig.class)
class RankingControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private RankingService servicio;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UserDetailsService userDetailsService;

    @TestConfiguration
    @EnableMethodSecurity
    static class SeguridadTestConfig {
    }

    private Usuario usuario(Role rol) {
        Usuario usuario = new Usuario("privado@example.com", "hash", "Nombre privado", rol);
        usuario.setId(1L);
        return usuario;
    }

    private RankingResponse respuesta() {
        return new RankingResponse("UNI", LocalDate.of(2026, 11, 2), true, "Lucero7", 3L, 120,
                new RankingPaginaResponse(List.of(new RankingEntradaResponse(1, "Ana", 300, false)),
                        12, 2, 0, 10));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void cualquierRolPuedeLeerConElContratoExacto(Role rol) throws Exception {
        when(servicio.obtenerRanking(0, 10)).thenReturn(respuesta());

        mvc.perform(get("/api/v1/ranking").with(user(usuario(rol))))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"universidad":"UNI","semanaInicio":"2026-11-02","participa":true,
                         "alias":"Lucero7","miPosicion":3,"miXpSemanal":120,
                         "entradas":{"content":[{"posicion":1,"alias":"Ana","xpSemanal":300,
                         "esUsuarioActual":false}],"totalElements":12,"totalPages":2,"number":0,"size":10}}
                        """, true));
    }

    @Test
    void lasTresOperacionesExigenAutenticacion() throws Exception {
        mvc.perform(get("/api/v1/ranking")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/ranking/participacion").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"alias\":\"Lucero7\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/ranking/participacion").with(csrf())).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"alias\":null}", "{\"alias\":\"   \"}", "{\"alias\":\" ab \"}",
            "{\"alias\":\"abcdefghijklmnopqrstu\"}", "{\"alias\":\"ana@x\"}", "{\"alias\":\"Ana😀\"}",
            "{\"alias\":\"Ana\\nLuz\"}", "{\"alias\":\"Ana/Luz\"}"})
    void rechazaAliasInvalidosCon400(String cuerpo) throws Exception {
        mvc.perform(put("/api/v1/ranking/participacion").with(user(usuario(Role.USER))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        verifyNoInteractions(servicio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ana", "Ñandú 7._-", "abcdefghijklmnopqrst", "ÁÉÍÓÚ ñ", "Jose\u0301"})
    void aceptaLetrasAcentosYCaracteresPermitidosDespuesDeRecortar(String alias) throws Exception {
        when(servicio.participar(any())).thenReturn(respuesta());

        mvc.perform(put("/api/v1/ranking/participacion").with(user(usuario(Role.USER))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"alias\":\"  " + alias + "  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entradas.size").value(10));
        verify(servicio).participar(new ParticipacionRankingRequest(alias));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "REVIEWER"})
    void elPersonalRecibe403AlIntentarParticipar(Role rol) throws Exception {
        mvc.perform(put("/api/v1/ranking/participacion").with(user(usuario(rol))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"alias\":\"Lucero7\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    void unAliasDuplicadoDevuelve409ConMensajeClaro() throws Exception {
        when(servicio.participar(any())).thenThrow(new DuplicateResourceException("Ese alias ya esta en uso"));

        mvc.perform(put("/api/v1/ranking/participacion").with(user(usuario(Role.USER))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"alias\":\"Lucero7\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Ese alias ya esta en uso"));
    }

    @Test
    void unConflictoDeVersionDevuelve409() throws Exception {
        when(servicio.participar(any())).thenThrow(new ObjectOptimisticLockingFailureException(Usuario.class, 1L));

        mvc.perform(put("/api/v1/ranking/participacion").with(user(usuario(Role.USER))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"alias\":\"Lucero7\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void unaViolacionDeIntegridadDevuelve409() throws Exception {
        when(servicio.participar(any())).thenThrow(new DataIntegrityViolationException("unique"));

        mvc.perform(put("/api/v1/ranking/participacion").with(user(usuario(Role.USER))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"alias\":\"Lucero7\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void retirarseDosVecesDevuelve204SinCuerpo() throws Exception {
        for (int intento = 0; intento < 2; intento++) {
            mvc.perform(delete("/api/v1/ranking/participacion").with(user(usuario(Role.USER))).with(csrf()))
                    .andExpect(status().isNoContent()).andExpect(content().string(""));
        }
        verify(servicio, times(2)).dejarDeParticipar();
    }
}
