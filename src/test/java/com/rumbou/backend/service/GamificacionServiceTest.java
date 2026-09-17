package com.rumbou.backend.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// JUnit puro, sin Spring: la funcion de racha es pura (no toca base de datos),
// igual que el CalificadorService se prueba con parametros.
class GamificacionServiceTest {

    private final GamificacionService gamificacionService = new GamificacionService(null, null, null);

    static Stream<Arguments> casosDeRacha() {
        LocalDate hoy = LocalDate.of(2026, 9, 13);
        return Stream.of(
                // rachaActual, ultimaActividad, hoy, rachaEsperada
                Arguments.of(0, null, hoy, 1),                 // primera actividad: arranca en 1
                Arguments.of(5, hoy, hoy, 5),                  // ya jugo hoy: no cambia ni se duplica
                Arguments.of(4, hoy.minusDays(1), hoy, 5),     // ayer: dia consecutivo, +1
                Arguments.of(7, hoy.minusDays(3), hoy, 1)      // se falto mas de un dia: se reinicia
        );
    }

    @ParameterizedTest
    @MethodSource("casosDeRacha")
    void calculaLaRachaCorrecta(int rachaActual, LocalDate ultimaActividad, LocalDate hoy,
                                int rachaEsperada) {
        int resultado = gamificacionService.calcularNuevaRacha(rachaActual, ultimaActividad, hoy);

        assertThat(resultado).isEqualTo(rachaEsperada);
    }
}