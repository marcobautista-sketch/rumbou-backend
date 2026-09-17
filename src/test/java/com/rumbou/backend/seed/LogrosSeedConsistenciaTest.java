package com.rumbou.backend.seed;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// logros.csv debe cuadrar con el catalogo que espera GamificacionService; lee con las reglas del runner real.
class LogrosSeedConsistenciaTest {

    @Test
    void elArchivoTieneLosLogrosEsperados() {
        List<ArchivoSeed.Fila> filas = ArchivoSeed.leer("logros.csv", 4);
        assertThat(filas).hasSize(5);
    }

    @Test
    void hayUnoPorCondicionEsperada() {
        List<ArchivoSeed.Fila> filas = ArchivoSeed.leer("logros.csv", 4);
        long primerSimulacro = filas.stream().filter(f -> "PRIMER_SIMULACRO".equals(f.texto(2))).count();
        long rachas = filas.stream().filter(f -> "RACHA_DIAS".equals(f.texto(2))).count();
        long xp = filas.stream().filter(f -> "XP_TOTAL".equals(f.texto(2))).count();

        assertThat(primerSimulacro).isEqualTo(1);
        assertThat(rachas).isEqualTo(2);
        assertThat(xp).isEqualTo(2);
    }

    @Test
    void losValoresRequeridosSonPositivos() {
        List<ArchivoSeed.Fila> filas = ArchivoSeed.leer("logros.csv", 4);
        for (ArchivoSeed.Fila fila : filas) {
            assertThat(fila.entero(3)).isPositive();
        }
    }
}