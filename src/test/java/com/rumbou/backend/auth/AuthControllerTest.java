package com.rumbou.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest solo levanta la capa web (controller + validacion), no carga
// SecurityConfig ni la base de datos: aqui probamos que los DTOs invalidos
// se rechacen con 400 y que el controller llame al service correctamente.
// addFilters = false desactiva los filtros de seguridad (incluido CSRF) en
// este slice: la seguridad real de /api/v1/auth/** (publica) ya esta
// definida en SecurityConfig y no es lo que estamos probando aqui.
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // JwtAuthenticationFilter es un @Component (implementa Filter), asi que
    // Spring lo instancia igual al armar el contexto aunque el slice sea
    // solo de este controller: sin estos mocks no puede construirse (le
    // faltan sus propias dependencias). addFilters=false ya evita que se
    // ejecute contra las requests, pero el bean igual debe poder crearse.
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void registerFallaConEmailInvalido() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.rumbou.backend.auth.dto.RegisterRequest("no-es-un-email", "password123", "Ana")
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginFallaSiFaltaLaContrasena() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.rumbou.backend.auth.dto.LoginRequest("postulante@rumbou.com", "")
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPasswordRespondeOkYDelegaAlServiceConEmailValido() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.rumbou.backend.auth.dto.ForgotPasswordRequest("postulante@rumbou.com")
        );

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(authService).forgotPassword("postulante@rumbou.com");
    }

    @Test
    void forgotPasswordFallaConEmailMalFormado() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.rumbou.backend.auth.dto.ForgotPasswordRequest("no-es-un-email")
        );

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(authService, never()).forgotPassword(anyString());
    }

    @Test
    void resetPasswordFallaSiFaltaElToken() throws Exception {
        String body = "{\"token\": \"\", \"newPassword\": \"nueva-contrasena\"}";

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPasswordFallaSiLaContrasenaEsMuyCorta() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.rumbou.backend.auth.dto.ResetPasswordRequest("token-123", "corta")
        );

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPasswordRespondeOkYDelegaAlServiceConDatosValidos() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.rumbou.backend.auth.dto.ResetPasswordRequest("token-123", "nueva-contrasena")
        );

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(authService).resetPassword("token-123", "nueva-contrasena");
    }
}
