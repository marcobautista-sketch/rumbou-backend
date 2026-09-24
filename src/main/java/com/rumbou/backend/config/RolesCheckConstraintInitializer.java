package com.rumbou.backend.config;

import com.rumbou.backend.entity.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// Hibernate crea un CHECK con los valores del enum Role al crear la tabla, pero
// ddl-auto=update no lo actualiza cuando el enum crece. Al arrancar se reemplaza
// ese CHECK por uno con todos los roles actuales; es idempotente.
@Component
@Order(0)
public class RolesCheckConstraintInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RolesCheckConstraintInitializer.class);
    private static final String NOMBRE_CONSTRAINT = "usuarios_role_check";

    private final JdbcTemplate jdbcTemplate;

    public RolesCheckConstraintInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        actualizarConstraint();
    }

    public void actualizarConstraint() {
        List<String> existentes = jdbcTemplate.queryForList("""
                SELECT conname FROM pg_constraint
                WHERE conrelid = 'usuarios'::regclass AND contype = 'c'
                  AND pg_get_constraintdef(oid) LIKE '%role%'
                """, String.class);
        existentes.forEach(nombre -> jdbcTemplate.execute(
                "ALTER TABLE usuarios DROP CONSTRAINT \"" + nombre + "\""));

        String valores = Arrays.stream(Role.values())
                .map(role -> "'" + role.name() + "'")
                .collect(Collectors.joining(", "));
        jdbcTemplate.execute("ALTER TABLE usuarios ADD CONSTRAINT " + NOMBRE_CONSTRAINT
                + " CHECK (role IN (" + valores + "))");
        log.info("Restriccion de roles actualizada: {}", valores);
    }
}
