package com.rumbou.backend.seed;

import com.rumbou.backend.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AdminBootstrapRunnerTest {

    private final UsuarioService usuarioService = mock(UsuarioService.class);

    private AdminBootstrapRunner runner(String email, String password) {
        return new AdminBootstrapRunner(usuarioService, email, password, "Administrador");
    }

    @Test
    void conAmbasVariablesAseguraElAdmin() {
        runner("admin@rumbou.com", "secreta").run(new DefaultApplicationArguments());

        verify(usuarioService).asegurarAdmin("admin@rumbou.com", "secreta", "Administrador");
    }

    @Test
    void sinVariablesNoHaceNada() {
        runner("", "").run(new DefaultApplicationArguments());

        verify(usuarioService, never()).asegurarAdmin(any(), any(), any());
    }

    @Test
    void conSoloUnaDeLasDosVariablesNoCreaNada() {
        runner("admin@rumbou.com", "").run(new DefaultApplicationArguments());
        runner("", "secreta").run(new DefaultApplicationArguments());

        verify(usuarioService, never()).asegurarAdmin(any(), any(), any());
    }

    @Test
    void recortaEspaciosDelEmail() {
        runner("  admin@rumbou.com ", "secreta").run(new DefaultApplicationArguments());

        verify(usuarioService).asegurarAdmin("admin@rumbou.com", "secreta", "Administrador");
    }
}
