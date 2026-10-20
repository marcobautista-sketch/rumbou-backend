package com.rumbou.backend.service;

import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.TipoLogro;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.repository.LogroRepository;
import com.rumbou.backend.repository.UsuarioLogroRepository;
import com.rumbou.backend.repository.UsuarioRepository;
import com.rumbou.backend.service.impl.GamificacionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GamificacionServiceTest {

    private final GamificacionServiceImpl gamificacionService = new GamificacionServiceImpl(null, null, null, null);

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

    @Test
    void elDesafioDiarioSumaXpYRachaSinElLogroDePrimerSimulacro() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        LogroRepository logroRepository = mock(LogroRepository.class);
        GamificacionServiceImpl servicio = new GamificacionServiceImpl(
                usuarioRepository, logroRepository, mock(UsuarioLogroRepository.class), null);
        Usuario ana = new Usuario("ana@rumbou.com", "hash", "Ana", Role.USER);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana));

        servicio.registrarActividad(1L, 50);

        assertThat(ana.getXpTotal()).isEqualTo(50);
        assertThat(ana.getCurrentStreak()).isEqualTo(1);
        verify(logroRepository, never()).findByCondicion(TipoLogro.PRIMER_SIMULACRO);
    }
}
