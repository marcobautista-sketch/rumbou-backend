package com.rumbou.backend.controller;

import com.rumbou.backend.dto.response.UsuarioResponse;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.InvalidOperationException;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.security.JwtService;
import com.rumbou.backend.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest no carga SecurityConfig: @EnableMethodSecurity se agrega aparte para que @PreAuthorize funcione.
@WebMvcTest(UsuarioController.class)
@Import(UsuarioControllerTest.MethodSecurityTestConfig.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private Usuario usuarioConRol(Long id, Role role) {
        Usuario usuario = new Usuario("postulante@rumbou.com", "hash", "Ana", role);
        usuario.setId(id);
        return usuario;
    }

    @Test
    void meDevuelveLosDatosDelUsuarioDelToken() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me").with(user(usuarioConRol(7L, Role.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("postulante@rumbou.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void sinTokenMeResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unUsuarioNormalNoPuedeCambiarRoles() throws Exception {
        mockMvc.perform(patch("/api/v1/usuarios/2/rol")
                        .with(user(usuarioConRol(1L, Role.USER)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).cambiarRol(any(), any(), any());
    }

    @Test
    void unAdminPuedePromoverAOtroUsuario() throws Exception {
        given(usuarioService.cambiarRol(eq(2L), eq(Role.ADMIN), any()))
                .willReturn(new UsuarioResponse(2L, "ana@rumbou.com", "Ana", Role.ADMIN));

        mockMvc.perform(patch("/api/v1/usuarios/2/rol")
                        .with(user(usuarioConRol(1L, Role.ADMIN)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void cambiarRolSinRolEnElCuerpoResponde400() throws Exception {
        mockMvc.perform(patch("/api/v1/usuarios/2/rol")
                        .with(user(usuarioConRol(1L, Role.ADMIN)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quitarseElPropioRolResponde400() throws Exception {
        given(usuarioService.cambiarRol(eq(1L), eq(Role.USER), any()))
                .willThrow(new InvalidOperationException("No puedes quitarte tu propio rol de administrador"));

        mockMvc.perform(patch("/api/v1/usuarios/1/rol")
                        .with(user(usuarioConRol(1L, Role.ADMIN)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unAdminPuedeBuscarUnUsuarioPorEmail() throws Exception {
        given(usuarioService.buscarPorEmail("ana@rumbou.com"))
                .willReturn(new UsuarioResponse(2L, "ana@rumbou.com", "Ana", Role.USER));

        mockMvc.perform(get("/api/v1/usuarios").param("email", "ana@rumbou.com")
                        .with(user(usuarioConRol(1L, Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void buscarUnEmailQueNoExisteResponde404() throws Exception {
        given(usuarioService.buscarPorEmail("nadie@rumbou.com"))
                .willThrow(new ResourceNotFoundException("No existe el usuario con email nadie@rumbou.com"));

        mockMvc.perform(get("/api/v1/usuarios").param("email", "nadie@rumbou.com")
                        .with(user(usuarioConRol(1L, Role.ADMIN))))
                .andExpect(status().isNotFound());
    }

    @Test
    void unUsuarioNormalNoPuedeBuscarPorEmail() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").param("email", "ana@rumbou.com")
                        .with(user(usuarioConRol(1L, Role.USER))))
                .andExpect(status().isForbidden());
    }
}
