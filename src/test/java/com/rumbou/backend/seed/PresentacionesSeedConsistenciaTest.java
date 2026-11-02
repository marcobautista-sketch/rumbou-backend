package com.rumbou.backend.seed;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// Los controles de presentaciones.json como test: una ficha que no coincide con preguntas.csv
// o que viene incompleta falla aqui, no en produccion.
class PresentacionesSeedConsistenciaTest {

    private static Set<String> clavesDelBanco() {
        Set<String> claves = new HashSet<>();
        for (ArchivoSeed.Fila fila : ArchivoSeed.leer("preguntas.csv", 11)) {
            claves.add(fila.texto(0) + "|" + fila.texto(2));
        }
        return claves;
    }

    @Test
    void cadaFichaCorrespondeAUnaPreguntaDelBancoSinRepetirse() {
        Set<String> banco = clavesDelBanco();
        Set<String> vistas = new HashSet<>();
        for (PresentacionesSeed.Ficha ficha : PresentacionesSeed.leer()) {
            String clave = ficha.tema() + "|" + ficha.enunciado();
            assertThat(banco).as("pregunta de la ficha").contains(clave);
            assertThat(vistas.add(clave)).as("ficha repetida: " + clave).isTrue();
        }
    }

    @Test
    void lasAlternativasConFormatoSonCincoCuandoVienen() {
        for (PresentacionesSeed.Ficha ficha : PresentacionesSeed.leer()) {
            List<String> alternativas = ficha.presentacion().alternativas();
            if (alternativas != null) {
                assertThat(alternativas).hasSize(5).allSatisfy(alt -> assertThat(alt).isNotBlank());
            }
        }
    }

    @Test
    void cadaFichaTieneDeDosACuatroPasosCompletos() {
        for (PresentacionesSeed.Ficha ficha : PresentacionesSeed.leer()) {
            assertThat(ficha.presentacion().pasos())
                    .hasSizeBetween(2, 4)
                    .allSatisfy(paso -> {
                        assertThat(paso.titulo()).isNotBlank();
                        assertThat(paso.detalle()).isNotBlank();
                    });
        }
    }
}
