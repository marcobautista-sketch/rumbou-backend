package com.rumbou.backend.seed;

import com.rumbou.backend.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Crea el primer administrador al arrancar, a partir de ADMIN_EMAIL y
// ADMIN_PASSWORD. Sin esas variables no hace nada, asi que en local y en los
// tests es inofensivo. Es la unica forma de tener un ADMIN sin entrar a la
// base de datos a mano; los siguientes se promueven con PATCH /usuarios/{id}/rol.
@Component
@Order(0)
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UsuarioService usuarioService;
    private final String email;
    private final String password;
    private final String nombre;

    public AdminBootstrapRunner(UsuarioService usuarioService,
                                @Value("${admin.email:}") String email,
                                @Value("${admin.password:}") String password,
                                @Value("${admin.nombre:Administrador}") String nombre) {
        this.usuarioService = usuarioService;
        this.email = email;
        this.password = password;
        this.nombre = nombre;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean hayEmail = !email.isBlank();
        boolean hayPassword = !password.isBlank();

        if (!hayEmail && !hayPassword) {
            return;
        }
        if (hayEmail != hayPassword) {
            log.warn("ADMIN_EMAIL y ADMIN_PASSWORD deben definirse juntas; no se crea el administrador");
            return;
        }

        usuarioService.asegurarAdmin(email.trim(), password, nombre);
    }
}
