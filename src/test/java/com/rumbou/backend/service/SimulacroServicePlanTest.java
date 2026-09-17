package com.rumbou.backend.service;

import com.rumbou.backend.dto.request.IniciarSimulacroRequest;
import com.rumbou.backend.entity.Area;
import com.rumbou.backend.entity.Funcionalidad;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Simulacro;
import com.rumbou.backend.entity.TipoSimulacro;
import com.rumbou.backend.entity.Universidad;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.UnauthorizedException;
import com.rumbou.backend.repository.AreaRepository;
import com.rumbou.backend.repository.EstructuraExamenRepository;
import com.rumbou.backend.repository.RespuestaUsuarioRepository;
import com.rumbou.backend.repository.SimulacroRepository;
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

// Iniciar pasa por PlanService; el diagnostico cuenta como simulacro completo.
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