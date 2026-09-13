package com.rumbou.backend.suscripcion;

import com.rumbou.backend.auth.Role;
import com.rumbou.backend.auth.Usuario;
import com.rumbou.backend.shared.exception.UnauthorizedException;
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

// JUnit puro con Mockito: PlanService no necesita levantar Spring,
// sus colaboradores (los dos repos) se simulan con datos prefabricados.
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
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void elTutorEsExclusivoDelPlanPro() {
        assertThatThrownBy(() -> planService.puedeAcceder(usuarioConId(7), Funcionalidad.TUTOR_IA))
                .isInstanceOf(UnauthorizedException.class);
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
                .isInstanceOf(UnauthorizedException.class);
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
                .isInstanceOf(UnauthorizedException.class);
    }

    // ---- registrarUso ----

    @Test
    void registrarUsoCreaLaFilaDelDiaSiNoExisteYSuma() {
        when(usoDiarioRepository.findByUsuarioIdAndFecha(anyLong(), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        // Instruccion especial de ensayo: "cuando te pidan guardar algo, devuelveme
        // exactamente ese objeto" (un mock por defecto devuelve null).
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
}