package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.EstadoPreparacion;
import com.rumbou.backend.dto.response.ObjetivoResponse;
import com.rumbou.backend.entity.Area;
import com.rumbou.backend.entity.Carrera;
import com.rumbou.backend.entity.ObjetivoUsuario;
import com.rumbou.backend.entity.OfertaAcademica;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Universidad;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.DuplicateResourceException;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.exception.UnauthorizedException;
import com.rumbou.backend.repository.ObjetivoUsuarioRepository;
import com.rumbou.backend.repository.OfertaAcademicaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProgresoServiceTest {

    private ObjetivoUsuarioRepository objetivoUsuarioRepository;
    private OfertaAcademicaRepository ofertaAcademicaRepository;
    private PlanService planService;
    private ProgresoService progresoService;

    private Usuario postulante;
    private Area areaGeneralUni;
    private OfertaAcademica sistemasUni;

    @BeforeEach
    void setUp() {
        objetivoUsuarioRepository = mock(ObjetivoUsuarioRepository.class);
        ofertaAcademicaRepository = mock(OfertaAcademicaRepository.class);
        planService = mock(PlanService.class);
        progresoService = new ProgresoService(objetivoUsuarioRepository, ofertaAcademicaRepository, planService);

        postulante = new Usuario("ana@rumbou.com", "hash", "Ana", Role.USER);
        postulante.setId(7L);

        Universidad uni = new Universidad("Universidad Nacional de Ingenieria", "UNI", 1800, 180);
        areaGeneralUni = new Area(uni, "GENERAL", "General");
        areaGeneralUni.setId(2L);
        Carrera sistemas = new Carrera("Ingenieria de Sistemas", "Facultad de Ingenieria Industrial y de Sistemas");
        sistemasUni = new OfertaAcademica(uni, sistemas, areaGeneralUni, "2026-II", 1209, 31);
        sistemasUni.setId(10L);

        when(objetivoUsuarioRepository.save(any(ObjetivoUsuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---- IP y semaforo (funciones puras) ----

    @ParameterizedTest
    @CsvSource({
            "1209, 1209, 1.0",   // justo en el corte
            "1330, 1209, 1.1",   // 10 % por encima
            "1000, 2000, 0.5",   // escala UNMSM
            "-50, 1000, -0.05"   // las penalidades pueden dejar el PSP negativo
    })
    void calculaElIpDividiendoElPspEntreElCorte(double psp, double corte, double ipEsperado) {
        assertThat(progresoService.calcularIp(psp, corte)).isCloseTo(ipEsperado, within(0.0001));
    }

    @Test
    void rechazaUnCorteQueNoSeaPositivo() {
        assertThatThrownBy(() -> progresoService.calcularIp(1000, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "1.25, HOLGADO",
            "1.10, HOLGADO",
            "1.0999, AJUSTADO",
            "1.00, AJUSTADO",
            "0.9999, CERCA",
            "0.85, CERCA",
            "0.8499, REFORZAR",
            "-0.2, REFORZAR"
    })
    void elSemaforoRespetaLosCortesDelDiseno(double ip, EstadoPreparacion esperado) {
        assertThat(EstadoPreparacion.desde(ip)).isEqualTo(esperado);
    }

    // ---- crear objetivos ----

    @Test
    void unObjetivoNuevoQuedaActivoYSinProgresoTodavia() {
        prepararCreacion(Optional.empty(), 0, 1);

        ObjetivoResponse respuesta = progresoService.crearObjetivo(postulante, 10L);

        assertThat(respuesta.carrera()).isEqualTo("Ingenieria de Sistemas");
        assertThat(respuesta.universidad()).isEqualTo("UNI");
        assertThat(respuesta.puntajeUltimoIngresante()).isEqualTo(1209);
        assertThat(respuesta.ultimoPsp()).isNull();
        assertThat(respuesta.estado()).isNull();
        verify(objetivoUsuarioRepository).save(any(ObjetivoUsuario.class));
    }

    @Test
    void crearUnObjetivoDeUnaOfertaInexistenteDa404() {
        when(ofertaAcademicaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> progresoService.crearObjetivo(postulante, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void elegirUnaCarreraQueYaEsObjetivoActivoDa409() {
        ObjetivoUsuario activo = new ObjetivoUsuario(postulante, sistemasUni, LocalDateTime.now());
        prepararCreacion(Optional.of(activo), 1, 3);

        assertThatThrownBy(() -> progresoService.crearObjetivo(postulante, 10L))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void unUsuarioGratuitoConUnObjetivoActivoNoPuedeCrearOtro() {
        prepararCreacion(Optional.empty(), 1, 1);
        when(planService.tieneAccesoPro(postulante)).thenReturn(false);

        assertThatThrownBy(() -> progresoService.crearObjetivo(postulante, 10L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("plan gratuito");
        verify(objetivoUsuarioRepository, never()).save(any(ObjetivoUsuario.class));
    }

    @Test
    void unUsuarioProPuedeTenerHastaTresObjetivos() {
        prepararCreacion(Optional.empty(), 2, 3);

        progresoService.crearObjetivo(postulante, 10L);

        verify(objetivoUsuarioRepository).save(any(ObjetivoUsuario.class));
    }

    @Test
    void unUsuarioProConTresObjetivosNoPuedeCrearUnCuarto() {
        prepararCreacion(Optional.empty(), 3, 3);
        when(planService.tieneAccesoPro(postulante)).thenReturn(true);

        assertThatThrownBy(() -> progresoService.crearObjetivo(postulante, 10L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("hasta 3");
    }

    @Test
    void elegirDeNuevoUnObjetivoDesactivadoReutilizaLaMismaFila() {
        ObjetivoUsuario desactivado = new ObjetivoUsuario(postulante, sistemasUni, LocalDateTime.now());
        desactivado.setId(5L);
        desactivado.setActivo(false);
        prepararCreacion(Optional.of(desactivado), 0, 1);

        ObjetivoResponse respuesta = progresoService.crearObjetivo(postulante, 10L);

        assertThat(respuesta.id()).isEqualTo(5L);
        assertThat(desactivado.isActivo()).isTrue();
        verify(objetivoUsuarioRepository).save(desactivado);
    }

    // ---- desactivar ----

    @Test
    void desactivarUnObjetivoInexistenteDa404() {
        when(objetivoUsuarioRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> progresoService.desactivarObjetivo(postulante, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void desactivarUnObjetivoAjenoDa403() {
        Usuario otro = new Usuario("luis@rumbou.com", "hash", "Luis", Role.USER);
        otro.setId(8L);
        ObjetivoUsuario ajeno = new ObjetivoUsuario(otro, sistemasUni, LocalDateTime.now());
        when(objetivoUsuarioRepository.findById(5L)).thenReturn(Optional.of(ajeno));

        assertThatThrownBy(() -> progresoService.desactivarObjetivo(postulante, 5L))
                .isInstanceOf(UnauthorizedException.class);
        assertThat(ajeno.isActivo()).isTrue();
    }

    @Test
    void desactivarUnObjetivoPropioLoDejaInactivo() {
        ObjetivoUsuario propio = new ObjetivoUsuario(postulante, sistemasUni, LocalDateTime.now());
        when(objetivoUsuarioRepository.findById(5L)).thenReturn(Optional.of(propio));

        progresoService.desactivarObjetivo(postulante, 5L);

        assertThat(propio.isActivo()).isFalse();
    }

    // ---- actualizacion tras un simulacro ----

    @Test
    void trasUnSimulacroGuardaElPspYElIpEnLosObjetivosDeEsaArea() {
        ObjetivoUsuario objetivo = new ObjetivoUsuario(postulante, sistemasUni, LocalDateTime.now());
        when(objetivoUsuarioRepository.findByUsuarioIdAndActivoTrueAndOfertaAcademicaAreaId(7L, 2L))
                .thenReturn(List.of(objetivo));
        when(objetivoUsuarioRepository.findByUsuarioIdAndActivoTrue(7L)).thenReturn(List.of(objetivo));

        progresoService.actualizarTrasSimulacro(7L, 2L, 1330);

        assertThat(objetivo.getUltimoPsp()).isEqualTo(1330);
        assertThat(objetivo.getUltimoIp()).isCloseTo(1.1, within(0.0001));
        assertThat(objetivo.getFechaActualizacion()).isNotNull();

        ObjetivoResponse enElPanel = progresoService.listarObjetivos(postulante).get(0);
        assertThat(enElPanel.estado()).isEqualTo(EstadoPreparacion.HOLGADO);
        assertThat(enElPanel.estadoDescripcion()).isEqualTo("Zona de ingreso holgada");
    }

    @Test
    void trasUnSimulacroSinObjetivosEnEsaAreaNoHaceNadaNiFalla() {
        when(objetivoUsuarioRepository.findByUsuarioIdAndActivoTrueAndOfertaAcademicaAreaId(7L, 2L))
                .thenReturn(List.of());

        progresoService.actualizarTrasSimulacro(7L, 2L, 1330);

        verify(objetivoUsuarioRepository, never()).save(any(ObjetivoUsuario.class));
        verify(objetivoUsuarioRepository).saveAll(anyList());
    }

    private void prepararCreacion(Optional<ObjetivoUsuario> existente, long activos, int limite) {
        when(ofertaAcademicaRepository.findById(10L)).thenReturn(Optional.of(sistemasUni));
        when(objetivoUsuarioRepository.findByUsuarioIdAndOfertaAcademicaId(7L, 10L)).thenReturn(existente);
        when(objetivoUsuarioRepository.countByUsuarioIdAndActivoTrue(7L)).thenReturn(activos);
        when(planService.limiteObjetivosActivos(postulante)).thenReturn(limite);
    }
}
