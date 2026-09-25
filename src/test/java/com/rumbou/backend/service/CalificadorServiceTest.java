package com.rumbou.backend.service;

import com.rumbou.backend.entity.AreaConocimiento;
import com.rumbou.backend.entity.Dificultad;
import com.rumbou.backend.entity.EsquemaCalificacion;
import com.rumbou.backend.entity.OrigenPregunta;
import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.entity.RespuestaUsuario;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.entity.Universidad;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CalificadorServiceTest {

    private static final int CLAVE = 2;

    private final CalificadorService calificadorService = new CalificadorService();

    static Stream<Arguments> esquemasReales() {
        return Stream.of(
                Arguments.of(6.0, 1.20, 80, 15, 5, 462.0),      // UNI, prueba de Aptitud
                Arguments.of(15.0, 3.00, 30, 8, 2, 426.0),      // UNI, prueba de Matematica
                Arguments.of(20.0, 1.125, 50, 15, 5, 983.125),  // UNMSM, seccion Conocimientos
                Arguments.of(20.0, 0.0, 8, 2, 0, 160.0)         // UNMSM, seccion Actitudinal (sin penalidad)
        );
    }

    @ParameterizedTest
    @MethodSource("esquemasReales")
    void calificaElSimulacroSegunElEsquema(double valorAcierto, double valorPenalidad,
                                           int correctas, int incorrectas, int blancos,
                                           double puntajeEsperado) {
        EsquemaCalificacion esquema = new EsquemaCalificacion(
                null, "Bloque de prueba", valorAcierto, valorPenalidad, 0, 0);
        Tema tema = new Tema("Tema de prueba", AreaConocimiento.MATEMATICA);
        tema.setId(1L);
        Pregunta pregunta = new Pregunta(tema, "Enunciado", List.of("a", "b", "c", "d", "e"), CLAVE,
                null, Dificultad.MEDIA, OrigenPregunta.SEMILLA, true);

        List<RespuestaUsuario> respuestas = new ArrayList<>();
        agregar(respuestas, pregunta, CLAVE, correctas);
        agregar(respuestas, pregunta, CLAVE + 1, incorrectas);
        agregar(respuestas, pregunta, null, blancos);

        double resultado = calificadorService.calificarSimulacro(respuestas, Map.of(1L, esquema));

        assertThat(resultado).isCloseTo(puntajeEsperado, within(0.001));
    }

    private static void agregar(List<RespuestaUsuario> respuestas, Pregunta pregunta, Integer marcada, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            respuestas.add(new RespuestaUsuario(null, pregunta, marcada, null));
        }
    }

    @Test
    void unaRespuestaEnBlancoNoSumaNiResta() {
        EsquemaCalificacion esquema = new EsquemaCalificacion(null, "Bloque", 6.0, 1.20, 0, 0);
        RespuestaUsuario respuestaEnBlanco = new RespuestaUsuario(null, null, null, null);

        calificadorService.calificarRespuesta(respuestaEnBlanco, esquema);

        assertThat(respuestaEnBlanco.getPuntajeAportado()).isZero();
        assertThat(respuestaEnBlanco.getEsCorrecta()).isNull();
    }

    @Test
    void elPspEscalaElPuntajeAlMaximoDeLaUniversidad() {
        Universidad uni = new Universidad("Universidad Nacional de Ingenieria", "UNI", 1800, 180);

        double psp = calificadorService.calcularPsp(900, 900, uni);

        assertThat(psp).isCloseTo(1800.0, within(0.001));
    }
}
