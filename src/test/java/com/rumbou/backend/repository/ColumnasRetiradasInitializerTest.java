package com.rumbou.backend.repository;

import com.rumbou.backend.config.ColumnasRetiradasInitializer;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.UsoDiario;
import com.rumbou.backend.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// Simula una base creada antes de retirar la columna: obligatoria y sin valor por defecto.
@Import(ColumnasRetiradasInitializer.class)
class ColumnasRetiradasInitializerTest extends AbstractContainerBaseTest {

    @Autowired
    private ColumnasRetiradasInitializer initializer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsoDiarioRepository usoDiarioRepository;

    @Test
    void quitaLaColumnaYPermiteRegistrarElUsoDelDia() {
        jdbcTemplate.execute("ALTER TABLE uso_diario ADD COLUMN consultas_tutoria integer NOT NULL");

        initializer.quitarColumnas();

        Usuario usuario = usuarioRepository.saveAndFlush(
                new Usuario("uso@rumbou.com", "hash", "Uso", Role.USER));
        UsoDiario uso = usoDiarioRepository.saveAndFlush(new UsoDiario(usuario, LocalDate.now()));
        assertThat(uso.getId()).isNotNull();
    }

    @Test
    void esIdempotente() {
        initializer.quitarColumnas();
        initializer.quitarColumnas();

        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name = 'uso_diario' AND column_name = 'consultas_tutoria'
                """, Integer.class)).isZero();
    }
}
