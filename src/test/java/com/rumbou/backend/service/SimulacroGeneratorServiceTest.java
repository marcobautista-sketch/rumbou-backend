package com.rumbou.backend.service;

import com.rumbou.backend.entity.Area;
import com.rumbou.backend.entity.AreaConocimiento;
import com.rumbou.backend.entity.Dificultad;
import com.rumbou.backend.entity.EsquemaCalificacion;
import com.rumbou.backend.entity.EstructuraExamen;
import com.rumbou.backend.entity.OrigenPregunta;
import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.entity.RespuestaUsuario;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.entity.TipoSimulacro;
import com.rumbou.backend.entity.Universidad;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.InvalidOperationException;
import com.rumbou.backend.repository.EstructuraExamenRepository;
import com.rumbou.backend.repository.PreguntaRepository;
import com.rumbou.backend.repository.RespuestaUsuarioRepository;
import com.rumbou.backend.repository.SimulacroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// JUnit puro con Mockito: que preguntas entran en el simulacro segun su tipo.
// Un area de prueba con dos temas, para poder distinguir el examen completo
// (los dos temas) del simulacro por tema (solo uno).
class SimulacroGeneratorServiceTest {

    private static final long TEMA_ALGEBRA_ID = 1L;
    private static final long TEMA_FISICA_ID = 2L;

    private EstructuraExamenRepository estructuraExamenRepository;
    private PreguntaRepository preguntaRepository;
    private RespuestaUsuarioRepository respuestaUsuarioRepository;
    private SimulacroGeneratorService generador;

    private Usuario usuario;
    private Area area;

    @BeforeEach
    void setUp() {
        estructuraExamenRepository = mock(EstructuraExamenRepository.class);
        preguntaRepository = mock(PreguntaRepository.class);
        respuestaUsuarioRepository = mock(RespuestaUsuarioRepository.class);

        generador = new SimulacroGeneratorService(
                estructuraExamenRepository,
                preguntaRepository,
                mock(SimulacroRepository.class),
                respuestaUsuarioRepository);

        usuario = new Usuario("postulante@rumbou.com", "hash", "Ana", Role.USER);
        usuario.setId(1L);

        Universidad universidad = new Universidad("Universidad de prueba", "UDP", 2000, 100);
        area = new Area(universidad, "A", "Area de prueba");
        area.setId(10L);

        Tema algebra = tema(TEMA_ALGEBRA_ID, "Algebra");
        Tema fisica = tema(TEMA_FISICA_ID, "Fisica");

        EsquemaCalificacion esquema = new EsquemaCalificacion(universidad, "Bloque unico", 20.0, 1.125, 2000, 1);

        when(estructuraExamenRepository.findByAreaIdOrderByOrden(10L)).thenReturn(List.of(
                new EstructuraExamen(area, esquema, algebra, 3, 1),
                new EstructuraExamen(area, esquema, fisica, 2, 2)));

        when(preguntaRepository.findByTemaIdAndAprobadaTrue(TEMA_ALGEBRA_ID))
                .thenReturn(preguntas(algebra, 10));
        when(preguntaRepository.findByTemaIdAndAprobadaTrue(TEMA_FISICA_ID))
                .thenReturn(preguntas(fisica, 10));
    }

    private Tema tema(long id, String nombre) {
        Tema tema = new Tema(nombre, AreaConocimiento.MATEMATICA);
        tema.setId(id);
        return tema;
    }

    // Lista modificable: el generador la baraja antes de elegir.
    private List<Pregunta> preguntas(Tema tema, int cantidad) {
        List<Pregunta> preguntas = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            preguntas.add(new Pregunta(tema, "Enunciado " + i, List.of("a", "b", "c", "d", "e"), 0,
                    "Explicacion", Dificultad.MEDIA, OrigenPregunta.SEMILLA, true));
        }
        return preguntas;
    }

    private List<RespuestaUsuario> respuestasGuardadas() {
        ArgumentCaptor<RespuestaUsuario> captor = ArgumentCaptor.forClass(RespuestaUsuario.class);
        verify(respuestaUsuarioRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void elSimulacroCompletoUsaTodosLosTemasDelArea() {
        generador.generar(usuario, area, TipoSimulacro.COMPLETO, null);

        List<RespuestaUsuario> respuestas = respuestasGuardadas();
        assertThat(respuestas).hasSize(5);
        assertThat(respuestas).extracting(r -> r.getPregunta().getTema().getId())
                .containsOnly(TEMA_ALGEBRA_ID, TEMA_FISICA_ID);
    }

    @Test
    void elDiagnosticoTambienUsaTodosLosTemas() {
        generador.generar(usuario, area, TipoSimulacro.DIAGNOSTICO, null);

        assertThat(respuestasGuardadas()).hasSize(5);
    }

    @Test
    void elSimulacroPorTemaSoloTraePreguntasDeEseTema() {
        generador.generar(usuario, area, TipoSimulacro.POR_TEMA, TEMA_FISICA_ID);

        List<RespuestaUsuario> respuestas = respuestasGuardadas();
        assertThat(respuestas).hasSize(2);
        assertThat(respuestas).extracting(r -> r.getPregunta().getTema().getId())
                .containsOnly(TEMA_FISICA_ID);
    }

    @Test
    void elSimulacroPorTemaRespetaLaCantidadDeEseTemaEnElExamenReal() {
        generador.generar(usuario, area, TipoSimulacro.POR_TEMA, TEMA_ALGEBRA_ID);

        assertThat(respuestasGuardadas()).hasSize(3);
    }

    @Test
    void unSimulacroPorTemaSinTemaIdEsUnError() {
        assertThatThrownBy(() -> generador.generar(usuario, area, TipoSimulacro.POR_TEMA, null))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("temaId");

        verify(respuestaUsuarioRepository, never()).save(any());
    }

    @Test
    void unTemaQueNoEstaEnElExamenDelAreaEsUnError() {
        assertThatThrownBy(() -> generador.generar(usuario, area, TipoSimulacro.POR_TEMA, 999L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("no forma parte del examen");

        verify(respuestaUsuarioRepository, never()).save(any());
    }

    @Test
    void elTemaIdSeIgnoraCuandoElTipoNoEsPorTema() {
        generador.generar(usuario, area, TipoSimulacro.COMPLETO, TEMA_FISICA_ID);

        assertThat(respuestasGuardadas()).hasSize(5);
    }

    @Test
    void sinPreguntasAprobadasAvisaEnVezDeDevolverUnSimulacroVacio() {
        when(preguntaRepository.findByTemaIdAndAprobadaTrue(TEMA_FISICA_ID)).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> generador.generar(usuario, area, TipoSimulacro.POR_TEMA, TEMA_FISICA_ID))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("No hay preguntas aprobadas");
    }
}
