package com.rumbou.backend.service;

import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.repository.LogroRepository;
import com.rumbou.backend.repository.UsuarioLogroRepository;
import com.rumbou.backend.repository.UsuarioRepository;
import com.rumbou.backend.security.CurrentUserService;
import com.rumbou.backend.service.impl.GamificacionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GamificacionSemanalTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final UsuarioLogroRepository logrosUsuario = mock(UsuarioLogroRepository.class);
    private final CurrentUserService usuarioActual = mock(CurrentUserService.class);

    static Stream<Arguments> semanas() {
        return Stream.of(
                Arguments.of("2026-11-04", null, 0),
                Arguments.of("2026-11-04", "2026-11-02", 450),
                Arguments.of("2026-11-04", "2026-11-04", 450),
                Arguments.of("2026-11-09", "2026-11-08", 0),
                Arguments.of("2026-01-01", "2025-12-31", 450),
                Arguments.of("2026-01-01", "2025-12-28", 0),
                Arguments.of("2026-01-05", "2026-01-04", 0),
                Arguments.of("2026-11-04", "2026-09-13", 0));
    }

    private GamificacionServiceImpl servicio(Instant instante) {
        return new GamificacionServiceImpl(usuarios, mock(LogroRepository.class), logrosUsuario,
                usuarioActual, Clock.fixed(instante, ZoneOffset.UTC));
    }

    private Usuario usuario(String ultimaActividad) {
        Usuario usuario = new Usuario("test@example.com", "hash", "Nombre privado", Role.USER);
        usuario.setId(1L);
        usuario.setXpTotal(1500);
        usuario.setXpSemanal(450);
        usuario.setCurrentStreak(3);
        usuario.setLongestStreak(8);
        usuario.setLastActivityDate(ultimaActividad == null ? null : LocalDate.parse(ultimaActividad));
        return usuario;
    }

    @ParameterizedTest
    @MethodSource("semanas")
    void reiniciaSoloElSaldoSemanalAntesDeSumar(String hoy, String anterior, int saldoVigente) {
        Usuario usuario = usuario(anterior);
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));

        servicio(Instant.parse(hoy + "T15:00:00Z")).registrarActividad(1L, 50);

        assertThat(usuario.getXpSemanal()).isEqualTo(saldoVigente + 50);
        assertThat(usuario.getXpTotal()).isEqualTo(1550);
        assertThat(usuario.getLastActivityDate()).isEqualTo(LocalDate.parse(hoy));
        assertThat(usuario.getLongestStreak()).isEqualTo(8);
    }

    @ParameterizedTest
    @MethodSource("semanas")
    void leerNormalizaSinCambiarLaEntidad(String hoy, String anterior, int saldoVigente) {
        Usuario usuario = usuario(anterior);
        when(usuarioActual.getUsuario()).thenReturn(usuario);

        var resumen = servicio(Instant.parse(hoy + "T15:00:00Z")).obtenerResumen();

        assertThat(resumen.xpSemanal()).isEqualTo(saldoVigente);
        assertThat(resumen.xpTotal()).isEqualTo(1500);
        assertThat(usuario.getXpSemanal()).isEqualTo(450);
        assertThat(usuario.getLastActivityDate()).isEqualTo(anterior == null ? null : LocalDate.parse(anterior));
        verifyNoInteractions(usuarios);
        verify(logrosUsuario, never()).save(any());
    }

    @Test
    void elCambioDeSemanaNoInterrumpeLaRachaDeDomingoALunes() {
        Usuario usuario = usuario("2026-11-08");
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));

        servicio(Instant.parse("2026-11-09T05:00:00Z")).procesarSimulacroFinalizado(1L);

        assertThat(usuario.getXpSemanal()).isEqualTo(100);
        assertThat(usuario.getXpTotal()).isEqualTo(1600);
        assertThat(usuario.getCurrentStreak()).isEqualTo(4);
    }

    @Test
    void elLunesUtcTodaviaPuedeSerDomingoEnLima() {
        Usuario usuario = usuario("2026-11-02");
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioActual.getUsuario()).thenReturn(usuario);
        var servicio = servicio(Instant.parse("2026-11-09T04:59:59Z"));

        assertThat(servicio.obtenerResumen().xpSemanal()).isEqualTo(450);
        servicio.registrarActividad(1L, 50);

        assertThat(usuario.getLastActivityDate()).isEqualTo(LocalDate.of(2026, 11, 8));
        assertThat(usuario.getXpSemanal()).isEqualTo(500);
    }
}
