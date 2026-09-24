package com.rumbou.backend.service;

import com.rumbou.backend.entity.EstadoSuscripcion;
import com.rumbou.backend.entity.Funcionalidad;
import com.rumbou.backend.entity.Plan;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Suscripcion;
import com.rumbou.backend.entity.UsoDiario;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.ForbiddenException;
import com.rumbou.backend.repository.SuscripcionRepository;
import com.rumbou.backend.repository.UsoDiarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlanServiceTest {

    private SuscripcionRepository suscripcionRepository;
    private UsoDiarioRepository usoDiarioRepository;
    private PlanService planService;

    @BeforeEach
    void setUp() {
        suscripcionRepository = mock(SuscripcionRepository.class);
        usoDiarioRepository = mock(UsoDiarioRepository.class);
        planService = new PlanService(suscripcionRepository, usoDiarioRepository);
    }

    private Usuario usuarioConId(long id) {
        Usuario usuario = new Usuario("postulante" + id + "@rumbou.com", "hash", "Ana", Role.USER);
        usuario.setId(id);
        return usuario;
    }

    private Usuario adminConId(long id) {
        Usuario admin = new Usuario("admin" + id + "@rumbou.com", "hash", "Marco", Role.ADMIN);
        admin.setId(id);
        return admin;
    }

    private Suscripcion suscripcionProActiva() {
        Suscripcion s = new Suscripcion(null, Plan.PRO, EstadoSuscripcion.ACTIVA);
        s.setFechaInicio(LocalDate.now().minusMonths(1));
        s.setFechaFin(LocalDate.now().plusMonths(1));
        return s;
    }

    private void activarPro() {
        when(suscripcionRepository.findFirstByUsuarioIdAndEstadoOrderByFechaInicioDesc(
                anyLong(), any(EstadoSuscripcion.class)))
                .thenReturn(Optional.of(suscripcionProActiva()));
    }

    // ---- esPro ----

    @Test
    void esProEsVerdaderoConSuscripcionActivaYVigente() {
        activarPro();

        assertThat(planService.esPro(7L)).isTrue();
    }

    @Test
    void esProEsVerdaderoSiLaFechaFinEsNula() {
        Suscripcion s = new Suscripcion(null, Plan.PRO, EstadoSuscripcion.ACTIVA);
        s.setFechaFin(null);
        when(suscripcionRepository.findFirstByUsuarioIdAndEstadoOrderByFechaInicioDesc(
                anyLong(), any(EstadoSuscripcion.class)))
                .thenReturn(Optional.of(s));

        assertThat(planService.esPro(7L)).isTrue();
    }

    @Test
    void esProEsFalsoSinSuscripcionActiva() {
        when(suscripcionRepository.findFirstByUsuarioIdAndEstadoOrderByFechaInicioDesc(
                anyLong(), any(EstadoSuscripcion.class)))
                .thenReturn(Optional.empty());

        assertThat(planService.esPro(7L)).isFalse();
    }

    @Test
    void esProEsFalsoSiLaSuscripcionEstaVencida() {
        Suscripcion vencida = new Suscripcion(null, Plan.PRO, EstadoSuscripcion.ACTIVA);
        vencida.setFechaFin(LocalDate.now().minusDays(1));
        when(suscripcionRepository.findFirstByUsuarioIdAndEstadoOrderByFechaInicioDesc(
                anyLong(), any(EstadoSuscripcion.class)))
                .thenReturn(Optional.of(vencida));

        assertThat(planService.esPro(7L)).isFalse();
    }

    // ---- TUTOR_IA ----

    @Test
    void proPuedeConsultarElTutorConMenosDe30Consultas() {
        activarPro();
        when(usoDiarioRepository.sumConsultasTutorIA(anyLong(), any(LocalDate.class))).thenReturn(29);

        planService.puedeAcceder(usuarioConId(7), Funcionalidad.TUTOR_IA);
    }

    @Test
    void proQuedaBloqueadoAlLlegarA30ConsultasAlTutor() {
        activarPro();
        when(usoDiarioRepository.sumConsultasTutorIA(anyLong(), any(LocalDate.class))).thenReturn(30);

        assertThatThrownBy(() -> planService.puedeAcceder(usuarioConId(7), Funcionalidad.TUTOR_IA))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void elTutorEsExclusivoDelPlanPro() {
        assertThatThrownBy(() -> planService.puedeAcceder(usuarioConId(7), Funcionalidad.TUTOR_IA))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---- SIMULACRO_TEMA ----

    @Test
    void proTieneSimulacrosDeTemaIlimitados() {
        activarPro();
        when(usoDiarioRepository.sumSimulacrosTemaDesde(anyLong(), any(LocalDate.class))).thenReturn(500);

        planService.puedeAcceder(usuarioConId(7), Funcionalidad.SIMULACRO_TEMA);
    }

    @Test
    void gratuitoPuedePracticarSiLlevaMenosDe3EnLaSemana() {
        when(usoDiarioRepository.sumSimulacrosTemaDesde(anyLong(), any(LocalDate.class))).thenReturn(2);

        planService.puedeAcceder(usuarioConId(7), Funcionalidad.SIMULACRO_TEMA);
    }

    @Test
    void gratuitoQuedaBloqueadoAlLlegarA3SimulacrosDeTema() {
        when(usoDiarioRepository.sumSimulacrosTemaDesde(anyLong(), any(LocalDate.class))).thenReturn(3);

        assertThatThrownBy(() -> planService.puedeAcceder(usuarioConId(7), Funcionalidad.SIMULACRO_TEMA))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---- SIMULACRO_COMPLETO ----

    @Test
    void gratuitoPuedeHacerSuPrimerSimulacroCompletoDelMes() {
        when(usoDiarioRepository.sumSimulacrosCompletosDesde(anyLong(), any(LocalDate.class))).thenReturn(0);

        planService.puedeAcceder(usuarioConId(7), Funcionalidad.SIMULACRO_COMPLETO);
    }

    @Test
    void gratuitoQuedaBloqueadoDespuesDelPrimerSimulacroCompletoDelMes() {
        when(usoDiarioRepository.sumSimulacrosCompletosDesde(anyLong(), any(LocalDate.class))).thenReturn(1);

        assertThatThrownBy(() -> planService.puedeAcceder(usuarioConId(7), Funcionalidad.SIMULACRO_COMPLETO))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---- registrarUso ----

    // ---- ADMIN sin limites ----

    @Test
    void unAdminSinSuscripcionPuedeUsarElTutorIa() {
        when(suscripcionRepository.findFirstByUsuarioIdAndEstadoOrderByFechaInicioDesc(
                anyLong(), any(EstadoSuscripcion.class)))
                .thenReturn(Optional.empty());

        planService.puedeAcceder(adminConId(1), Funcionalidad.TUTOR_IA);

        org.mockito.Mockito.verifyNoInteractions(usoDiarioRepository);
    }

    @Test
    void unAdminNoTieneTopeDeSimulacrosCompletosNiDeTema() {
        when(usoDiarioRepository.sumSimulacrosCompletosDesde(anyLong(), any(LocalDate.class))).thenReturn(99);
        when(usoDiarioRepository.sumSimulacrosTemaDesde(anyLong(), any(LocalDate.class))).thenReturn(99);

        planService.puedeAcceder(adminConId(1), Funcionalidad.SIMULACRO_COMPLETO);
        planService.puedeAcceder(adminConId(1), Funcionalidad.SIMULACRO_TEMA);
    }

    @Test
    void unAdminTampocoTieneElTopeDiarioDelTutorIa() {
        activarPro();
        when(usoDiarioRepository.sumConsultasTutorIA(anyLong(), any(LocalDate.class))).thenReturn(30);

        assertThatThrownBy(() -> planService.puedeAcceder(usuarioConId(7), Funcionalidad.TUTOR_IA))
                .isInstanceOf(ForbiddenException.class);
        planService.puedeAcceder(adminConId(1), Funcionalidad.TUTOR_IA);
    }

    @Test
    void registrarUsoCreaLaFilaDelDiaSiNoExisteYSuma() {
        when(usoDiarioRepository.findByUsuarioIdAndFecha(anyLong(), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(usoDiarioRepository.save(any(UsoDiario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        planService.registrarUso(usuarioConId(7), Funcionalidad.SIMULACRO_TEMA);

        org.mockito.Mockito.verify(usoDiarioRepository).save(any(UsoDiario.class));
    }

    @Test
    void registrarUsoIncrementaElContadorPedidoSobreLaFilaExistente() {
        UsoDiario hoy = new UsoDiario(usuarioConId(7), LocalDate.now());
        when(usoDiarioRepository.findByUsuarioIdAndFecha(anyLong(), any(LocalDate.class)))
                .thenReturn(Optional.of(hoy));

        planService.registrarUso(usuarioConId(7), Funcionalidad.TUTOR_IA);

        assertThat(hoy.getConsultasTutorIA()).isEqualTo(1);
        assertThat(hoy.getSimulacrosTema()).isZero();
    }

    // ---- acceso PRO y objetivos (progreso) ----

    @Test
    void tieneAccesoProEsVerdaderoParaUnUsuarioPro() {
        activarPro();

        assertThat(planService.tieneAccesoPro(usuarioConId(7))).isTrue();
    }

    @Test
    void tieneAccesoProEsFalsoParaUnUsuarioGratuito() {
        assertThat(planService.tieneAccesoPro(usuarioConId(7))).isFalse();
    }

    @Test
    void tieneAccesoProDejaPasarAlAdminSinConsultarSuSuscripcion() {
        assertThat(planService.tieneAccesoPro(adminConId(1))).isTrue();

        org.mockito.Mockito.verifyNoInteractions(suscripcionRepository);
    }

    @Test
    void elPlanGratuitoPermiteUnSoloObjetivoActivo() {
        assertThat(planService.limiteObjetivosActivos(usuarioConId(7))).isEqualTo(1);
    }

    @Test
    void elPlanProPermiteHastaTresObjetivosActivos() {
        activarPro();

        assertThat(planService.limiteObjetivosActivos(usuarioConId(7))).isEqualTo(3);
    }

    @Test
    void unAdminNoTieneTopeDeObjetivosActivos() {
        assertThat(planService.limiteObjetivosActivos(adminConId(1))).isEqualTo(Integer.MAX_VALUE);
    }
}