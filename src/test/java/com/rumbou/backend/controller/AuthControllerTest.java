package com.rumbou.backend.controller;

import com.rumbou.backend.dto.request.ForgotPasswordRequest;
import com.rumbou.backend.dto.request.LoginRequest;
import com.rumbou.backend.dto.request.RegisterRequest;
import com.rumbou.backend.dto.request.ResetPasswordRequest;
import com.rumbou.backend.dto.response.AuthResponse;
import com.rumbou.backend.security.JwtService;
import com.rumbou.backend.security.RefreshTokenCookie;
import com.rumbou.backend.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest: solo capa web, sin SecurityConfig ni BD. addFilters=false apaga los
// filtros (incluido CSRF): la seguridad de /api/v1/auth/** no es lo que se prueba aqui.
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RefreshTokenCookie.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // JwtAuthenticationFilter se instancia igual en el slice: necesita sus dependencias aunque no se ejecute.
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void registerFallaConEmailInvalido() throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterRequest("no-es-un-email", "Password123", "Ana")
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginFallaSiFaltaLaContrasena() throws Exception {
        String body = objectMapper.writeValueAsString(
                new LoginRequest("postulante@rumbou.com", "")
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPasswordResponde204YDelegaAlServiceConEmailValido() throws Exception {
        String body = objectMapper.writeValueAsString(
                new ForgotPasswordRequest("postulante@rumbou.com")
        );

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        verify(authService).forgotPassword("postulante@rumbou.com");
    }

    @Test
    void forgotPasswordFallaConEmailMalFormado() throws Exception {
        String body = objectMapper.writeValueAsString(
                new ForgotPasswordRequest("no-es-un-email")
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
                new ResetPasswordRequest("token-123", "corta")
        );

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPasswordResponde204YDelegaAlServiceConDatosValidos() throws Exception {
        String body = objectMapper.writeValueAsString(
                new ResetPasswordRequest("token-123", "NuevaClave2026")
        );

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        verify(authService).resetPassword("token-123", "NuevaClave2026");
    }

    @Test
    void registerFallaConUnaContrasenaSinMayusculasNiNumeros() throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterRequest("postulante@rumbou.com", "solominusculas", "Ana")
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loginEntregaElRefreshTokenEnUnaCookieHttpOnly() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("access", "refresh"));
        String body = objectMapper.writeValueAsString(
                new LoginRequest("postulante@rumbou.com", "Password123")
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(cookie().value(RefreshTokenCookie.NOMBRE, "refresh"))
                .andExpect(cookie().httpOnly(RefreshTokenCookie.NOMBRE, true))
                .andExpect(cookie().secure(RefreshTokenCookie.NOMBRE, true))
                .andExpect(cookie().path(RefreshTokenCookie.NOMBRE, "/api/v1/auth"))
                .andExpect(cookie().sameSite(RefreshTokenCookie.NOMBRE, "Strict"));
    }

    @Test
    void refreshAceptaElTokenDeLaCookieSinCuerpo() throws Exception {
        when(authService.refresh("refresh")).thenReturn(new AuthResponse("access-nuevo", "refresh-nuevo"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie(RefreshTokenCookie.NOMBRE, "refresh")))
                .andExpect(status().isOk())
                .andExpect(cookie().value(RefreshTokenCookie.NOMBRE, "refresh-nuevo"));
    }

    @Test
    void refreshSigueAceptandoElTokenEnElCuerpo() throws Exception {
        when(authService.refresh("refresh-movil")).thenReturn(new AuthResponse("access", "refresh"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"refresh-movil\"}"))
                .andExpect(status().isOk());

        verify(authService).refresh("refresh-movil");
    }
}
