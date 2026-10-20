package com.rumbou.backend.service;

import com.rumbou.backend.dto.response.DesafioDiarioResponse;
import com.rumbou.backend.entity.AreaConocimiento;
import com.rumbou.backend.entity.DesafioDiario;
import com.rumbou.backend.entity.Dificultad;
import com.rumbou.backend.entity.EstadoSimulacro;
import com.rumbou.backend.entity.OrigenPregunta;
import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.event.DesafioCompletadoEvent;
import com.rumbou.backend.exception.DuplicateResourceException;
import com.rumbou.backend.repository.DesafioDiarioRepository;
import com.rumbou.backend.repository.PreguntaRepository;
import com.rumbou.backend.repository.RespuestaUsuarioRepository;
import com.rumbou.backend.repository.RespuestaUsuarioRepository.ConteoPorTema;
import com.rumbou.backend.repository.TemaRepository;
import com.rumbou.backend.security.CurrentUserService;
import com.rumbou.backend.service.impl.DesafioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DesafioServiceTest {

    private DesafioDiarioRepository desafioDiarioRepository;
    private PreguntaRepository preguntaRepository;
    private TemaRepository temaRepository;
    private RespuestaUsuarioRepository respuestaUsuarioRepository;
    private ApplicationEventPublisher eventPublisher;
    private DesafioService desafioService;
    private Usuario ana;

    record Conteo(Long getTemaId, String getTema, Long getCorrectas, Long getIncorrectas, Long getEnBlanco)
            implements ConteoPorTema {
    }

    @BeforeEach
    void setUp() {
        desafioDiarioRepository = mock(DesafioDiarioRepository.class);
        preguntaRepository = mock(PreguntaRepository.class);
        temaRepository = mock(TemaRepository.class);
        respuestaUsuarioRepository = mock(RespuestaUsuarioRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        desafioService = new DesafioServiceImpl(desafioDiarioRepository, preguntaRepository, temaRepository,
                respuestaUsuarioRepository, currentUserService, eventPublisher);

        ana = new Usuario("ana@rumbou.com", "hash", "Ana", Role.USER);
        ana.setId(1L);
        when(currentUserService.getUsuario()).thenReturn(ana);
        when(desafioDiarioRepository.save(any(DesafioDiario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Tema tema(Long id, String nombre) {
        Tema tema = new Tema(nombre, AreaConocimiento.MATEMATICA);
        tema.setId(id);
        return tema;
    }

    private Pregunta pregunta(Long id, Tema tema) {
        Pregunta pregunta = new Pregunta(tema, "Enunciado " + id, List.of("a", "b", "c", "d", "e"), 2,
                "Porque si", Dificultad.MEDIA, OrigenPregunta.SEMILLA, true);
        pregunta.setId(id);
        return pregunta;
    }

    private DesafioDiario desafioDeHoy() {
        DesafioDiario desafio = new DesafioDiario(ana, LocalDate.now(), pregunta(10L, tema(1L, "Algebra")));
        when(desafioDiarioRepository.findByUsuarioIdAndFecha(1L, LocalDate.now())).thenReturn(Optional.of(desafio));
        return desafio;
    }

    @Test
    void laPrimeraConsultaDelDiaEligeUnaPreguntaDelTemaMasDebil() {
        Tema algebra = tema(1L, "Algebra");
        Tema fisica = tema(2L, "Fisica");
        when(desafioDiarioRepository.findByUsuarioIdAndFecha(1L, LocalDate.now())).thenReturn(Optional.empty());
        when(respuestaUsuarioRepository.contarRespuestasPorTema(1L, EstadoSimulacro.FINALIZADO)).thenReturn(List.of(
                new Conteo(2L, "Fisica", 9L, 1L, 0L),
                new Conteo(1L, "Algebra", 2L, 6L, 2L)));
        when(temaRepository.findAll()).thenReturn(List.of(algebra, fisica));
        when(preguntaRepository.findByTemaIdAndAprobadaTrue(1L)).thenReturn(List.of(pregunta(10L, algebra)));

        DesafioDiarioResponse desafio = desafioService.obtenerDeHoy();

        assertThat(desafio.tema()).isEqualTo("Algebra");
        assertThat(desafio.respondido()).isFalse();
        assertThat(desafio.claveCorrecta()).isNull();
    }

    @Test
    void siYaHayDesafioHoyDevuelveElMismo() {
        desafioDeHoy();

        DesafioDiarioResponse desafio = desafioService.obtenerDeHoy();

        assertThat(desafio.preguntaId()).isEqualTo(10L);
        verify(preguntaRepository, never()).findByTemaIdAndAprobadaTrue(anyLong());
    }

    @Test
    void acertarDa50XpYRevelaLaClave() {
        desafioDeHoy();

        DesafioDiarioResponse respuesta = desafioService.responder(2);

        assertThat(respuesta.esCorrecta()).isTrue();
        assertThat(respuesta.xpGanado()).isEqualTo(50);
        assertThat(respuesta.claveCorrecta()).isEqualTo(2);
        verify(eventPublisher).publishEvent(new DesafioCompletadoEvent(1L, 50));
    }

    @Test
    void elQuintoDesafioDeLaSemanaSumaElBono() {
        desafioDeHoy();
        when(desafioDiarioRepository.countByUsuarioIdAndFechaBetweenAndFechaRespuestaIsNotNull(
                any(), any(), any())).thenReturn(4L);

        DesafioDiarioResponse respuesta = desafioService.responder(0);

        assertThat(respuesta.esCorrecta()).isFalse();
        assertThat(respuesta.xpGanado()).isEqualTo(110);
        assertThat(respuesta.desafiosDeLaSemana()).isEqualTo(5);
    }

    @Test
    void noSePuedeResponderDosVecesElMismoDia() {
        desafioDeHoy().registrarRespuesta(2, true, 50);

        assertThatThrownBy(() -> desafioService.responder(1))
                .isInstanceOf(DuplicateResourceException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
