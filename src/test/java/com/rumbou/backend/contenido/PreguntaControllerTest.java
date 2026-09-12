package com.rumbou.backend.contenido;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumbou.backend.auth.JwtService;
import com.rumbou.backend.auth.Role;
import com.rumbou.backend.auth.Usuario;
import com.rumbou.backend.contenido.dto.CreatePreguntaRequest;
import com.rumbou.backend.contenido.dto.PreguntaAdminResponse;
import com.rumbou.backend.contenido.dto.PreguntaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest no carga SecurityConfig, asi que @EnableMethodSecurity se agrega
// aqui aparte para que @PreAuthorize funcione en el test.
@WebMvcTest(PreguntaController.class)
@Import(PreguntaControllerTest.MethodSecurityTestConfig.class)
class PreguntaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PreguntaService preguntaService;

    // JwtAuthenticationFilter es un Filter de Servlet, asi que @WebMvcTest lo carga
    // automaticamente y hay que darle sus dependencias para que pueda construirse.
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private CreatePreguntaRequest requestValido() {
        return new CreatePreguntaRequest(
                1L,
                "¿Cuanto es 2 + 2?",
                List.of("1", "2", "3", "4", "5"),
                3,
                "2 + 2 = 4",
                Dificultad.FACIL,
                OrigenPregunta.SEMILLA,
                true
        );
    }

    private Usuario usuarioConRol(Role role) {
        return new Usuario("postulante@rumbou.com", "hash", "Ana", role);
    }

    @Test
    void unUsuarioAutenticadoPuedeVerUnaPregunta() throws Exception {
        given(preguntaService.obtener(1L, false)).willReturn(
                new PreguntaResponse(1L, 1L, "Algebra", "¿Cuanto es 2 + 2?", List.of("1", "2", "3", "4", "5"), Dificultad.FACIL));

        mockMvc.perform(get("/api/v1/preguntas/1").with(user(usuarioConRol(Role.USER))))
                .andExpect(status().isOk());

        verify(preguntaService).obtener(1L, false);
    }

    @Test
    void unAdminQueVeUnaPreguntaPasaEsAdminEnTrue() throws Exception {
        given(preguntaService.obtener(1L, true)).willReturn(
                new PreguntaResponse(1L, 1L, "Algebra", "¿Cuanto es 2 + 2?", List.of("1", "2", "3", "4", "5"), Dificultad.FACIL));

        mockMvc.perform(get("/api/v1/preguntas/1").with(user(usuarioConRol(Role.ADMIN))))
                .andExpect(status().isOk());

        verify(preguntaService).obtener(1L, true);
    }

    @Test
    void alListarUnUsuarioSinRolAdminPasaEsAdminEnFalse() throws Exception {
        given(preguntaService.buscar(any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.eq(false)))
                .willReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/v1/preguntas?aprobada=false").with(user(usuarioConRol(Role.USER))))
                .andExpect(status().isOk());

        verify(preguntaService).buscar(any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.eq(false));
    }

    @Test
    void unAdminPuedeAprobarUnaPregunta() throws Exception {
        given(preguntaService.aprobar(1L)).willReturn(
                new PreguntaAdminResponse(1L, 1L, "Algebra", "¿Cuanto es 2 + 2?",
                        List.of("1", "2", "3", "4", "5"), 3, "2 + 2 = 4", Dificultad.FACIL, OrigenPregunta.SEMILLA, true));

        mockMvc.perform(patch("/api/v1/preguntas/1/aprobar")
                        .with(user(usuarioConRol(Role.ADMIN)))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void unUsuarioSinRolAdminNoPuedeAprobarPreguntas() throws Exception {
        mockMvc.perform(patch("/api/v1/preguntas/1/aprobar")
                        .with(user(usuarioConRol(Role.USER)))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void unUsuarioSinRolAdminNoPuedeCrearPreguntas() throws Exception {
        mockMvc.perform(post("/api/v1/preguntas")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unAdminPuedeCrearUnaPreguntaValida() throws Exception {
        given(preguntaService.crear(any())).willReturn(
                new PreguntaAdminResponse(1L, 1L, "Algebra", "¿Cuanto es 2 + 2?",
                        List.of("1", "2", "3", "4", "5"), 3, "2 + 2 = 4", Dificultad.FACIL, OrigenPregunta.SEMILLA, true));

        mockMvc.perform(post("/api/v1/preguntas")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rechazaUnaPreguntaConMenosDeCincoAlternativas() throws Exception {
        CreatePreguntaRequest invalido = new CreatePreguntaRequest(
                1L, "¿Cuanto es 2 + 2?", List.of("1", "2", "3"), 0,
                "explicacion", Dificultad.FACIL, OrigenPregunta.SEMILLA, true);

        mockMvc.perform(post("/api/v1/preguntas")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rechazaUnaClaveCorrectaFueraDeRango() throws Exception {
        CreatePreguntaRequest invalido = new CreatePreguntaRequest(
                1L, "¿Cuanto es 2 + 2?", List.of("1", "2", "3", "4", "5"), 9,
                "explicacion", Dificultad.FACIL, OrigenPregunta.SEMILLA, true);

        mockMvc.perform(post("/api/v1/preguntas")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest());
    }
}
