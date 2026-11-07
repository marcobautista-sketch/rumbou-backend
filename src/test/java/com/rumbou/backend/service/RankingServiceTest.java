package com.rumbou.backend.service;

import com.rumbou.backend.dto.request.ParticipacionRankingRequest;
import com.rumbou.backend.entity.*;
import com.rumbou.backend.exception.DuplicateResourceException;
import com.rumbou.backend.exception.ForbiddenException;
import com.rumbou.backend.repository.ObjetivoUsuarioRepository;
import com.rumbou.backend.repository.RankingRepository;
import com.rumbou.backend.repository.UsuarioRepository;
import com.rumbou.backend.security.CurrentUserService;
import com.rumbou.backend.service.impl.RankingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RankingServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ObjetivoUsuarioRepository objetivos = mock(ObjetivoUsuarioRepository.class);
    private final RankingRepository ranking = mock(RankingRepository.class);
    private final CurrentUserService usuarioActual = mock(CurrentUserService.class);
    private final RankingServiceImpl servicio = new RankingServiceImpl(usuarios, objetivos, ranking,
            usuarioActual, Clock.fixed(Instant.parse("2026-11-09T04:30:00Z"), ZoneOffset.UTC));
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuario = new Usuario("privado@example.com", "hash", "Nombre privado", Role.USER);
        usuario.setId(1L);
        usuario.setXpSemanal(120);
        usuario.setLastActivityDate(LocalDate.of(2026, 11, 2));
        when(usuarioActual.getUsuarioId()).thenReturn(1L);
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));
    }

    private void conMeta() {
        Universidad universidad = new Universidad("Universidad", "UNI", 1800, 180);
        universidad.setId(9L);
        OfertaAcademica oferta = new OfertaAcademica();
        oferta.setUniversidad(universidad);
        when(objetivos.findFirstByUsuarioIdAndActivoTrueOrderByIdAsc(1L))
                .thenReturn(Optional.of(new ObjetivoUsuario(usuario, oferta, LocalDateTime.now())));
        when(ranking.obtenerParticipantes(eq(9L), any(), any()))
                .thenAnswer(invocacion -> new PageImpl<>(List.of(), invocacion.getArgument(2), 0));
    }

    @Test
    void sinMetaDevuelvePaginaVaciaYNormalizaElSaldoSinEscribir() {
        usuario.setAliasLiga("Lucero7");
        usuario.setLastActivityDate(LocalDate.of(2026, 10, 25));

        var resultado = servicio.obtenerRanking(0, 10);

        assertThat(resultado.universidad()).isNull();
        assertThat(resultado.miPosicion()).isNull();
        assertThat(resultado.miXpSemanal()).isZero();
        assertThat(resultado.participa()).isTrue();
        assertThat(resultado.semanaInicio()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(resultado.entradas().content()).isEmpty();
        assertThat(resultado.entradas().totalElements()).isZero();
        assertThat(resultado.entradas().totalPages()).isZero();
        assertThat(usuario.getXpSemanal()).isEqualTo(120);
        verifyNoInteractions(ranking);
        verify(usuarios, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {51, 100, Integer.MAX_VALUE})
    void limitaElTamanoYNormalizaLaPaginaNegativa(int size) {
        conMeta();

        var resultado = servicio.obtenerRanking(-3, size);

        assertThat(resultado.entradas().size()).isEqualTo(50);
        assertThat(resultado.entradas().number()).isZero();
        verify(ranking).obtenerParticipantes(9L, LocalDate.of(2026, 11, 2), PageRequest.of(0, 50));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void usaDiezSiElTamanoNoEsPositivo(int size) {
        assertThat(servicio.obtenerRanking(0, size).entradas().size()).isEqualTo(10);
    }

    @Test
    void conservaLaPosicionAbsolutaEnPaginasPosteriores() {
        conMeta();
        RankingRepository.Participante participante = mock(RankingRepository.Participante.class);
        when(participante.getId()).thenReturn(1L);
        when(participante.getAlias()).thenReturn("Lucero7");
        when(participante.getXpSemanal()).thenReturn(120);
        when(ranking.obtenerParticipantes(9L, LocalDate.of(2026, 11, 2), PageRequest.of(1, 10)))
                .thenReturn(new PageImpl<>(List.of(participante), PageRequest.of(1, 10), 11));
        when(ranking.obtenerPosicion(9L, LocalDate.of(2026, 11, 2), 1L)).thenReturn(11L);

        var resultado = servicio.obtenerRanking(1, 10);

        assertThat(resultado.miPosicion()).isEqualTo(11L);
        assertThat(resultado.miXpSemanal()).isEqualTo(120);
        assertThat(resultado.entradas().content().getFirst().posicion()).isEqualTo(11);
        assertThat(resultado.entradas().content().getFirst().esUsuarioActual()).isTrue();
        assertThat(resultado.entradas().totalPages()).isEqualTo(2);
    }

    @Test
    void permiteConservarElAliasPropioYDevuelveLaPrimeraPagina() {
        usuario.setAliasLiga("Lucero7");

        var resultado = servicio.participar(new ParticipacionRankingRequest("  Lucero7  "));

        assertThat(resultado.alias()).isEqualTo("Lucero7");
        assertThat(resultado.participa()).isTrue();
        assertThat(resultado.entradas().number()).isZero();
        assertThat(resultado.entradas().size()).isEqualTo(10);
        var orden = inOrder(usuarios);
        orden.verify(usuarios).reservarAliasLiga("Lucero7");
        orden.verify(usuarios).existeAliasLigaDeOtroUsuario("Lucero7", 1L);
        orden.verify(usuarios).saveAndFlush(usuario);
    }

    @Test
    void rechazaElAliasDeOtroUsuario() {
        when(usuarios.existeAliasLigaDeOtroUsuario("LUCERO7", 1L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.participar(new ParticipacionRankingRequest("LUCERO7")))
                .isInstanceOf(DuplicateResourceException.class).hasMessageContaining("alias ya esta en uso");
        assertThat(usuario.getAliasLiga()).isNull();
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void traduceLaColisionDeLaRestriccionUnicaAConflicto() {
        when(usuarios.saveAndFlush(usuario)).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> servicio.participar(new ParticipacionRankingRequest("Lucero7")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void propagaElConflictoDeVersionAlManejadorGlobal() {
        when(usuarios.saveAndFlush(usuario)).thenThrow(new ObjectOptimisticLockingFailureException(Usuario.class, 1L));

        assertThatThrownBy(() -> servicio.participar(new ParticipacionRankingRequest("Lucero7")))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "REVIEWER"})
    void elPersonalPuedeLeerPeroNoInscribirse(Role rol) {
        usuario.setRole(rol);
        conMeta();

        assertThat(servicio.obtenerRanking(0, 10).universidad()).isEqualTo("UNI");
        assertThatThrownBy(() -> servicio.participar(new ParticipacionRankingRequest("Lucero7")))
                .isInstanceOf(ForbiddenException.class);
        verify(usuarios, never()).reservarAliasLiga(any());
    }

    @Test
    void retirarseEsIdempotenteInclusoSiAhoraEsPersonal() {
        usuario.setAliasLiga("Lucero7");
        usuario.setRole(Role.ADMIN);

        servicio.dejarDeParticipar();
        servicio.dejarDeParticipar();

        assertThat(usuario.getAliasLiga()).isNull();
        verify(usuarios, times(1)).saveAndFlush(usuario);
    }
}
