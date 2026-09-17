package com.rumbou.backend.seed;

import com.rumbou.backend.entity.Dificultad;
import com.rumbou.backend.entity.OrigenPregunta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

// Los controles de ContenidoSeedRunner como test: un preguntas.csv mal formado falla aqui, no en produccion.
class PreguntasSeedConsistenciaTest {

    @Test
    void elArchivoTieneElFormatoDeOnceColumnas() {
        assertThatCode(() -> ArchivoSeed.leer("preguntas.csv", 11)).doesNotThrowAnyException();
    }

    @Test
    void laClaveCorrectaEstaEntreCeroYCuatro() {
        for (ArchivoSeed.Fila fila : ArchivoSeed.leer("preguntas.csv", 11)) {
            assertThat(fila.entero(8)).isBetween(0, 4);
        }
    }

    @Test
    void laDificultadEsUnValorValido() {
        for (ArchivoSeed.Fila fila : ArchivoSeed.leer("preguntas.csv", 11)) {
            assertThatCode(() -> Dificultad.valueOf(fila.texto(1))).doesNotThrowAnyException();
        }
    }

    @Test
    void elOrigenEsUnValorValido() {
        for (ArchivoSeed.Fila fila : ArchivoSeed.leer("preguntas.csv", 11)) {
            assertThatCode(() -> OrigenPregunta.valueOf(fila.texto(10))).doesNotThrowAnyException();
        }
    }

    @Test
    void cadaPreguntaTieneExactamenteCincoAlternativasNoVacias() {
        for (ArchivoSeed.Fila fila : ArchivoSeed.leer("preguntas.csv", 11)) {
            List<String> alternativas = List.of(
                    fila.texto(3), fila.texto(4), fila.texto(5), fila.texto(6), fila.texto(7));
            assertThat(alternativas).hasSize(5).allSatisfy(alt -> assertThat(alt).isNotBlank());
        }
    }
}
