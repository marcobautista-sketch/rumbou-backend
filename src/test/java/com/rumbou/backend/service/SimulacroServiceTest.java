package com.rumbou.backend.service;

import com.rumbou.backend.dto.request.ResponderPreguntaRequest;
import com.rumbou.backend.entity.Area;
import com.rumbou.backend.entity.AreaConocimiento;
import com.rumbou.backend.entity.Dificultad;
import com.rumbou.backend.entity.EsquemaCalificacion;
import com.rumbou.backend.entity.EstadoSimulacro;
import com.rumbou.backend.entity.EstructuraExamen;
import com.rumbou.backend.entity.OrigenPregunta;
import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.entity.RespuestaUsuario;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Simulacro;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.entity.TipoSimulacro;
import com.rumbou.backend.entity.Universidad;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.event.RespuestaIncorrectaEvent;
import com.rumbou.backend.event.SimulacroFinalizadoEvent;
import com.rumbou.backend.exception.InvalidOperationException;
import com.rumbou.backend.exception.ForbiddenException;
import com.rumbou.backend.repository.AreaRepository;
import com.rumbou.backend.repository.EstructuraExamenRepository;
import com.rumbou.backend.repository.RespuestaUsuarioRepository;
import com.rumbou.backend.repository.SimulacroRepository;
import com.rumbou.backend.security.CurrentUserService;
import com.rumbou.backend.service.impl.SimulacroServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SimulacroServiceTest {

    private SimulacroRepository simulacroRepository;
    private RespuestaUsuarioRepository respuestaUsuarioRepository;
    private EstructuraExamenRepository estructuraExamenRepository;
    private ApplicationEventPublisher eventPublisher;
    private SimulacroServiceImpl simulacroService;
    private CurrentUserService currentUserService;

    private Usuario usuario;
    private Universidad universidad;
    private Area area;

    @BeforeEach
    void setUp() {
        currentUserService = mock(CurrentUserService.class);
        simulacroRepository = mock(SimulacroRepository.class);
        respuestaUsuarioRepository = mock(RespuestaUsuarioRepository.class);
        estructuraExamenRepository = mock(EstructuraExamenRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        simulacroService = new SimulacroServiceImpl(
                simulacroRepository,
                respuestaUsuarioRepository,
                mock(AreaRepository.class),
                estructuraExamenRepository,
                mock(SimulacroGeneratorService.class),
                new CalificadorService(),
                eventPublisher,
                mock(PlanService.class),
                currentUserService
        );

        usuario = new Usuario("postulante@rumbou.com", "hash", "Ana", Role.USER);
        usuario.setId(1L);
        when(currentUserService.getUsuario()).thenReturn(usuario);
        when(currentUserService.getUsuarioId()).thenReturn(1L);

        universidad = new Universidad("Universidad de prueba", "UDP", 2000, 100);
        universidad.setId(1L);

        area = new Area(universidad, "A", "Area de prueba");
        area.setId(10L);
    }

    private Tema tema(Long id, String nombre) {
        Tema tema = new Tema(nombre, AreaConocimiento.MATEMATICA);
        tema.setId(id);
        return tema;
    }

    private Pregunta pregunta(Long id, Tema tema, int claveCorrecta) {
        Pregunta pregunta = new Pregunta(tema, "Enunciado de prueba", List.of("a", "b", "c", "d", "e"),
                claveCorrecta, null, Dificultad.MEDIA, OrigenPregunta.SEMILLA, true);
        pregunta.setId(id);
        return pregunta;
    }

    private Simulacro simulacroEnCurso() {
        Simulacro simulacro = new Simulacro(usuario, area, TipoSimulacro.COMPLETO, LocalDateTime.now());
        simulacro.setId(100L);
        return simulacro;
    }

    private RespuestaUsuario respuesta(Long id, Simulacro simulacro, Pregunta pregunta, Integer marcada) {
        RespuestaUsuario respuesta = new RespuestaUsuario(simulacro, pregunta, marcada, LocalDateTime.now());
        respuesta.setId(id);
        return respuesta;
    }

    @Test
    void finalizarCalificaPublicaEventosYCierraElSimulacro() {
        Simulacro simulacro = simulacroEnCurso();
        Tema matematica = tema(1L, "Matematica");
        EsquemaCalificacion esquema = new EsquemaCalificacion(universidad, "Bloque unico", 20.0, 1.125, 100, 1);

        Pregunta acertada = pregunta(1L, matematica, 2);
        Pregunta fallada = pregunta(2L, matematica, 0);
        List<RespuestaUsuario> respuestas = List.of(
                respuesta(1L, simulacro, acertada, 2),
                respuesta(2L, simulacro, fallada, 4)
        );

        when(simulacroRepository.findById(100L)).thenReturn(Optional.of(simulacro));
        when(respuestaUsuarioRepository.findBySimulacroId(100L)).thenReturn(respuestas);
        when(estructuraExamenRepository.findByAreaIdOrderByOrden(10L))
                .thenReturn(List.of(new EstructuraExamen(area, esquema, matematica, 2, 1)));

        var resultado = simulacroService.finalizar(100L);

        // 1 acierto (+20) y 1 error (-1.125)
        assertThat(resultado.puntajeObtenido()).isEqualTo(18.875);
        assertThat(simulacro.getEstado()).isEqualTo(EstadoSimulacro.FINALIZADO);
        assertThat(simulacro.getFechaFin()).isNotNull();
        verify(eventPublisher).publishEvent(any(SimulacroFinalizadoEvent.class));
        verify(eventPublisher).publishEvent(any(RespuestaIncorrectaEvent.class));
    }

    @Test
    void responderRechazaUnaAlternativaQueNoExisteEnLaPregunta() {
        Simulacro simulacro = simulacroEnCurso();
        Pregunta preguntaDe5Alternativas = pregunta(1L, tema(1L, "Matematica"), 2);

        when(simulacroRepository.findById(100L)).thenReturn(Optional.of(simulacro));
        when(respuestaUsuarioRepository.findBySimulacroIdAndPreguntaId(100L, 1L))
                .thenReturn(Optional.of(respuesta(1L, simulacro, preguntaDe5Alternativas, null)));

        assertThatThrownBy(() -> simulacroService.responder(100L,
                new ResponderPreguntaRequest(1L, 9)))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void responderAceptaNullComoPreguntaEnBlanco() {
        Simulacro simulacro = simulacroEnCurso();
        RespuestaUsuario respuesta = respuesta(1L, simulacro, pregunta(1L, tema(1L, "Matematica"), 2), 3);

        when(simulacroRepository.findById(100L)).thenReturn(Optional.of(simulacro));
        when(respuestaUsuarioRepository.findBySimulacroIdAndPreguntaId(100L, 1L)).thenReturn(Optional.of(respuesta));

        simulacroService.responder(100L, new ResponderPreguntaRequest(1L, null));

        assertThat(respuesta.getAlternativaMarcada()).isNull();
    }

    @Test
    void finalizarFallaSiElSimulacroYaEstaCerrado() {
        Simulacro simulacro = simulacroEnCurso();
        simulacro.setEstado(EstadoSimulacro.FINALIZADO);
        when(simulacroRepository.findById(100L)).thenReturn(Optional.of(simulacro));

        assertThatThrownBy(() -> simulacroService.finalizar(100L))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void finalizarFallaSiElSimulacroEsDeOtroUsuario() {
        Usuario otro = new Usuario("otro@rumbou.com", "hash", "Beto", Role.USER);
        otro.setId(2L);
        when(simulacroRepository.findById(100L)).thenReturn(Optional.of(simulacroEnCurso()));
        when(currentUserService.getUsuarioId()).thenReturn(otro.getId());

        assertThatThrownBy(() -> simulacroService.finalizar(100L))
                .isInstanceOf(ForbiddenException.class);
    }

    // El seed puede poner el mismo Tema en dos bloques de un area; antes reventaba con IllegalStateException (500).
    @Test
    void finalizarAvisaConUnErrorClaroSiElAreaTieneElMismoTemaEnDosBloques() {
        Simulacro simulacro = simulacroEnCurso();
        Tema matematica = tema(1L, "Matematica");
        EsquemaCalificacion habilidades = new EsquemaCalificacion(universidad, "Habilidades", 20.0, 1.125, 100, 1);
        EsquemaCalificacion conocimientos = new EsquemaCalificacion(universidad, "Conocimientos", 15.0, 3.0, 100, 2);

        when(simulacroRepository.findById(100L)).thenReturn(Optional.of(simulacro));
        when(respuestaUsuarioRepository.findBySimulacroId(100L))
                .thenReturn(List.of(respuesta(1L, simulacro, pregunta(1L, matematica, 0), 0)));
        when(estructuraExamenRepository.findByAreaIdOrderByOrden(10L)).thenReturn(List.of(
                new EstructuraExamen(area, habilidades, matematica, 1, 1),
                new EstructuraExamen(area, conocimientos, matematica, 1, 2)
        ));

        assertThatThrownBy(() -> simulacroService.finalizar(100L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("mismo tema");
    }

    // Si la estructura cambio despues de generar el simulacro puede haber respuestas sin esquema; antes era un NPE (500).
    @Test
    void finalizarAvisaConUnErrorClaroSiUnTemaYaNoTieneEsquema() {
        Simulacro simulacro = simulacroEnCurso();
        Tema matematica = tema(1L, "Matematica");
        Tema fisica = tema(2L, "Fisica");
        EsquemaCalificacion esquema = new EsquemaCalificacion(universidad, "Bloque unico", 20.0, 1.125, 100, 1);

        when(simulacroRepository.findById(100L)).thenReturn(Optional.of(simulacro));
        when(respuestaUsuarioRepository.findBySimulacroId(100L))
                .thenReturn(List.of(respuesta(1L, simulacro, pregunta(1L, fisica, 0), 0)));
        when(estructuraExamenRepository.findByAreaIdOrderByOrden(10L))
                .thenReturn(List.of(new EstructuraExamen(area, esquema, matematica, 1, 1)));

        assertThatThrownBy(() -> simulacroService.finalizar(100L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("esquema");
    }

    @Test
    void elDetalleDeUnSimulacroEnCursoNoRevelaLaClave() {
        Simulacro simulacro = simulacroEnCurso();
        Pregunta pregunta = pregunta(1L, tema(1L, "Matematica"), 2);
        when(simulacroRepository.findById(100L)).thenReturn(Optional.of(simulacro));
        when(respuestaUsuarioRepository.findBySimulacroId(100L))
                .thenReturn(List.of(respuesta(1L, simulacro, pregunta, 3)));

        var detalle = simulacroService.obtener(100L);

        assertThat(detalle.preguntas()).hasSize(1);
        assertThat(detalle.preguntas().get(0).alternativaMarcada()).isEqualTo(3);
        assertThat(detalle.preguntas().get(0).claveCorrecta()).isNull();
        assertThat(detalle.preguntas().get(0).explicacion()).isNull();
    }

    @Test
    void elDetalleDeUnSimulacroFinalizadoIncluyeLaCorreccion() {
        Simulacro simulacro = simulacroEnCurso();
        simulacro.setEstado(EstadoSimulacro.FINALIZADO);
        Pregunta pregunta = pregunta(1L, tema(1L, "Matematica"), 2);
        RespuestaUsuario respuesta = respuesta(1L, simulacro, pregunta, 3);
        respuesta.setEsCorrecta(false);
        when(simulacroRepository.findById(100L)).thenReturn(Optional.of(simulacro));
        when(respuestaUsuarioRepository.findBySimulacroId(100L)).thenReturn(List.of(respuesta));

        var detalle = simulacroService.obtener(100L);

        assertThat(detalle.preguntas().get(0).claveCorrecta()).isEqualTo(2);
        assertThat(detalle.preguntas().get(0).esCorrecta()).isFalse();
    }

    @Test
    void noSePuedeVerElDetalleDeUnSimulacroAjeno() {
        when(simulacroRepository.findById(100L)).thenReturn(Optional.of(simulacroEnCurso()));
        when(currentUserService.getUsuarioId()).thenReturn(2L);

        assertThatThrownBy(() -> simulacroService.obtener(100L))
                .isInstanceOf(ForbiddenException.class);
    }
}
