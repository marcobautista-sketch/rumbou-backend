package com.rumbou.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// ddl-auto=update no borra columnas. Una columna obligatoria que ya no esta en la entidad
// haria fallar cada INSERT en las bases que aun la tienen, asi que se quita al arrancar.
// Es idempotente: en una base nueva no hace nada.
@Component
@Order(0)
public class ColumnasRetiradasInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public ColumnasRetiradasInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        quitarColumnas();
    }

    public void quitarColumnas() {
        jdbcTemplate.execute("ALTER TABLE uso_diario DROP COLUMN IF EXISTS consultas_tutoria");
    }
}
