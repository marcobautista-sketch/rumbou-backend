package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.DominioTemaResponse;
import com.rumbou.backend.dto.response.HistorialPspResponse;
import com.rumbou.backend.entity.Area;
import com.rumbou.backend.entity.EstadoSimulacro;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Simulacro;
import com.rumbou.backend.entity.TipoSimulacro;
import com.rumbou.backend.entity.Universidad;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.ForbiddenException;
import com.rumbou.backend.repository.RespuestaUsuarioRepository;
import com.rumbou.backend.repository.SimulacroRepository;
import com.rumbou.backend.security.CurrentUserService;
import com.rumbou.backend.service.impl.ProgresoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Funciones PRO del panel: dominio por tema e historial de PSP.
class ProgresoServiceTest {

    private SimulacroRepository simulacroRepository;
    private RespuestaUsuarioRepository respuestaUsuarioRepository;
    private PlanService planService;
    private ProgresoServiceImpl progresoService;

    private Usuario postulante;
    private Area areaGeneralUni;

    @BeforeEach
    void setUp() {
        simulacroRepository = mock(SimulacroRepository.class);
        respuestaUsuarioRepository = mock(RespuestaUsuarioRepository.class);
        planService = mock(PlanService.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        progresoService = new ProgresoServiceImpl(simulacroRepository, respuestaUsuarioRepository,
                planService, currentUserService);

        postulante = new Usuario("ana@rumbou.com", "hash", "Ana", Role.USER);
        postulante.setId(7L);
        when(currentUserService.getUsuario()).thenReturn(postulante);
        when(currentUserService.getUsuarioId()).thenReturn(7L);

        Universidad uni = new Universidad("Universidad Nacional de Ingenieria", "UNI", 1800, 180);
        areaGeneralUni = new Area(uni, "GENERAL", "General");
        areaGeneralUni.setId(2L);
    }

    @Test
    void elDominioPorTemaEsExclusivoDePro() {
        when(planService.tieneAccesoPro(postulante)).thenReturn(false);

        assertThatThrownBy(() -> progresoService.dominioPorTema())
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("PRO");
        verifyNoInteractions(respuestaUsuarioRepository);
    }

    @Test
    void elDominioCuentaLasPreguntasEnBlancoYOrdenaDelTemaMasDebilAlMasFuerte() {
        when(planService.tieneAccesoPro(postulante)).thenReturn(true);
        when(respuestaUsuarioRepository.contarRespuestasPorTema(7L, EstadoSimulacro.FINALIZADO))
                .thenReturn(List.of(conteo(1L, "Algebra", 8, 1, 1), conteo(2L, "Fisica", 3, 4, 3)));

        List<DominioTemaResponse> dominio = progresoService.dominioPorTema();

        assertThat(dominio).extracting(DominioTemaResponse::tema).containsExactly("Fisica", "Algebra");
        DominioTemaResponse fisica = dominio.get(0);
        assertThat(fisica.total()).isEqualTo(10);
        assertThat(fisica.enBlanco()).isEqualTo(3);
        assertThat(fisica.porcentajeAciertos()).isEqualTo(30.0);
        assertThat(dominio.get(1).porcentajeAciertos()).isEqualTo(80.0);
    }

    @Test
    void elHistoricoDePspEsExclusivoDePro() {
        when(planService.tieneAccesoPro(postulante)).thenReturn(false);

        assertThatThrownBy(() -> progresoService.historialPsp(2L))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(simulacroRepository);
    }

    @Test
    void elHistoricoDevuelveElPspDeCadaSimulacroFinalizado() {
        when(planService.tieneAccesoPro(postulante)).thenReturn(true);
        Simulacro primero = simulacroFinalizado(900.0, LocalDateTime.of(2026, 9, 10, 10, 0));
        Simulacro segundo = simulacroFinalizado(1100.0, LocalDateTime.of(2026, 9, 18, 10, 0));
        when(simulacroRepository.findByUsuarioIdAndAreaIdAndEstadoOrderByFechaFinAsc(
                7L, 2L, EstadoSimulacro.FINALIZADO)).thenReturn(List.of(primero, segundo));

        List<HistorialPspResponse> historial = progresoService.historialPsp(2L);

        assertThat(historial).extracting(HistorialPspResponse::psp).containsExactly(900.0, 1100.0);
        assertThat(historial.get(0).tipo()).isEqualTo(TipoSimulacro.COMPLETO);
    }

    private Simulacro simulacroFinalizado(double psp, LocalDateTime fechaFin) {
        Simulacro simulacro = new Simulacro(postulante, areaGeneralUni, TipoSimulacro.COMPLETO, fechaFin.minusHours(3));
        simulacro.setEstado(EstadoSimulacro.FINALIZADO);
        simulacro.setPsp(psp);
        simulacro.setPuntajeObtenido(psp / 2);
        simulacro.setFechaFin(fechaFin);
        return simulacro;
    }

    private RespuestaUsuarioRepository.ConteoPorTema conteo(Long temaId, String tema,
                                                           long correctas, long incorrectas, long enBlanco) {
        return new RespuestaUsuarioRepository.ConteoPorTema() {
            public Long getTemaId() {
                return temaId;
            }

            public String getTema() {
                return tema;
            }

            public Long getCorrectas() {
                return correctas;
            }

            public Long getIncorrectas() {
                return incorrectas;
            }

            public Long getEnBlanco() {
                return enBlanco;
            }
        };
    }
}
