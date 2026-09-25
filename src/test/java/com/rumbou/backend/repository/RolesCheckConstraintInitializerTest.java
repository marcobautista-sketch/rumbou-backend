package com.rumbou.backend.repository;

import com.rumbou.backend.config.RolesCheckConstraintInitializer;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

// Simula una base creada antes de existir REVIEWER: su CHECK solo admite USER y ADMIN.
@Import(RolesCheckConstraintInitializer.class)
class RolesCheckConstraintInitializerTest extends AbstractContainerBaseTest {

    @Autowired
    private RolesCheckConstraintInitializer initializer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void reemplazaElCheckViejoYPermiteGuardarUnRevisor() {
        initializer.actualizarConstraint();
        jdbcTemplate.execute("ALTER TABLE usuarios DROP CONSTRAINT usuarios_role_check");
        jdbcTemplate.execute("ALTER TABLE usuarios ADD CONSTRAINT chk_roles_viejo CHECK (role IN ('USER', 'ADMIN'))");

        initializer.actualizarConstraint();

        Usuario revisor = usuarioRepository.saveAndFlush(
                new Usuario("revisor@rumbou.com", "hash", "Rita", Role.REVIEWER));
        assertThat(revisor.getId()).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_roles_viejo'", Integer.class))
                .isZero();
    }

    @Test
    void esIdempotente() {
        initializer.actualizarConstraint();
        initializer.actualizarConstraint();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'usuarios_role_check'", Integer.class))
                .isEqualTo(1);
    }
}
