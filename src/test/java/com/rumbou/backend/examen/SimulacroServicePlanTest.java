package com.rumbou.backend.examen;

import com.rumbou.backend.academico.Area;
import com.rumbou.backend.academico.AreaRepository;
import com.rumbou.backend.academico.EstructuraExamenRepository;
import com.rumbou.backend.academico.Universidad;
import com.rumbou.backend.auth.Role;
import com.rumbou.backend.auth.Usuario;
import com.rumbou.backend.examen.dto.IniciarSimulacroRequest;
import com.rumbou.backend.shared.exception.UnauthorizedException;
import com.rumbou.backend.suscripcion.Funcionalidad;
import com.rumbou.backend.suscripcion.PlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// JUnit puro con Mockito: verifica que iniciar pasa por PlanService (la puerta
// unica de limites). El diagnostico se trata como simulacro completo.
class SimulacroServicePlanTest {

    private AreaRepository areaRepository;
    private SimulacroGeneratorService simulacroGeneratorService;
    private PlanService planService;
    private SimulacroService simulacroService;

    private Usuario usuario;
    private Area area;

    @BeforeEach
    void setUp() {
        areaRepository = mock(AreaRepository.class);
        simulacroGeneratorService = mock(SimulacroGeneratorService.class);
        planService = mock(PlanService.class);

        simulacroService = new SimulacroService(
                mock(SimulacroRepository.class),
                mock(RespuestaUsuarioRepository.class),
                areaRepository,
                mock(EstructuraExamenRepository.class),
                simulacroGeneratorService,
                new CalificadorService(),
                mock(ApplicationEventPublisher.class),
                planService
        );

        usuario = new Usuario("postulante@rumbou.com", "hash", "Ana", Role.USER);
        usuario.setId(1L);

        Universidad universidad = new Universidad("Universidad de prueba", "UDP", 2000, 100);
        area = new Area(universidad, "A", "Area de prueba");
        area.setId(10L);

        when(areaRepository.findById(10L)).thenReturn(Optional.of(area));
    }

    @Test
    void iniciarPorTemaVerificaLimiteYRegistraUso() {
        when(simulacroGeneratorService.generar(usuario, area, TipoSimulacro.POR_TEMA))
                .thenReturn(new Simulacro(usuario, area, TipoSimulacro.POR_TEMA, LocalDateTime.now()));

        simulacroService.iniciar(usuario,
                new IniciarSimulacroRequest(10L, TipoSimulacro.POR_TEMA));

        verify(planService).puedeAcceder(usuario, Funcionalidad.SIMULACRO_TEMA);
        verify(planService).registrarUso(usuario, Funcionalidad.SIMULACRO_TEMA);
    }

    @Test
    void iniciarCompletoUsaElContadorDeSimulacroCompleto() {
        when(simulacroGeneratorService.generar(usuario, area, TipoSimulacro.COMPLETO))
                .thenReturn(new Simulacro(usuario, area, TipoSimulacro.COMPLETO, LocalDateTime.now()));

        simulacroService.iniciar(usuario,
                new IniciarSimulacroRequest(10L, TipoSimulacro.COMPLETO));

        verify(planService).puedeAcceder(usuario, Funcionalidad.SIMULACRO_COMPLETO);
        verify(planService).registrarUso(usuario, Funcionalidad.SIMULACRO_COMPLETO);
    }

    @Test
    void iniciarDiagnosticoSeCobraComoSimulacroCompleto() {
        when(simulacroGeneratorService.generar(usuario, area, TipoSimulacro.DIAGNOSTICO))
                .thenReturn(new Simulacro(usuario, area, TipoSimulacro.DIAGNOSTICO, LocalDateTime.now()));

        simulacroService.iniciar(usuario,
                new IniciarSimulacroRequest(10L, TipoSimulacro.DIAGNOSTICO));

        verify(planService).puedeAcceder(usuario, Funcionalidad.SIMULACRO_COMPLETO);
        verify(planService).registrarUso(usuario, Funcionalidad.SIMULACRO_COMPLETO);
    }

    @Test
    void iniciarFallaCon403SiElPlanNoAlcanzaYNoRegistraUso() {
        doThrow(new UnauthorizedException("Limite alcanzado"))
                .when(planService).puedeAcceder(usuario, Funcionalidad.SIMULACRO_TEMA);

        assertThatThrownBy(() -> simulacroService.iniciar(usuario,
                new IniciarSimulacroRequest(10L, TipoSimulacro.POR_TEMA)))
                .isInstanceOf(UnauthorizedException.class);

        verify(planService, never()).registrarUso(usuario, Funcionalidad.SIMULACRO_TEMA);
    }
}