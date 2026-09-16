package com.rumbou.backend.contenido.gemini;

import com.rumbou.backend.academico.AreaConocimiento;
import com.rumbou.backend.academico.Tema;
import com.rumbou.backend.academico.TemaRepository;
import com.rumbou.backend.contenido.Dificultad;
import com.rumbou.backend.contenido.PreguntaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// JUnit + Mockito puro: prueba el recorrido de --todos y el salteo idempotente,
// sin llamar a Gemini de verdad. pausaEntreLlamadasMs=0 para que el test no espere.
class GeneradorPreguntasRunnerTest {

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private GeminiPreguntaValidator validator;

    @Mock
    private PreguntaRepository preguntaRepository;

    @Mock
    private TemaRepository temaRepository;

    private GeneradorPreguntasRunner runner;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        runner = new GeneradorPreguntasRunner(geminiClient, validator, preguntaRepository, temaRepository, 0);
    }

    private Tema temaConId(Long id, String nombre) {
        Tema tema = new Tema(nombre, AreaConocimiento.MATEMATICA, null);
        tema.setId(id);
        return tema;
    }

    private PreguntaGeneradaDto preguntaValida() {
        return new PreguntaGeneradaDto(
                "¿Cuanto es 2 + 2?", List.of("1", "2", "3", "4", "5"), 3, "2 + 2 = 4");
    }

    @Test
    void generarTodosRecorreTodosLosTemasYLasTresDificultades() {
        Tema tema1 = temaConId(1L, "Algebra");
        Tema tema2 = temaConId(2L, "Geometria");
        given(temaRepository.findAll()).willReturn(List.of(tema1, tema2));
        given(preguntaRepository.countByTemaIdAndDificultad(any(), any())).willReturn(0L);
        given(geminiClient.generarPregunta(any(), any(), any())).willReturn(preguntaValida());
        given(validator.validar(any())).willReturn(Optional.empty());

        runner.generarTodos(1);

        // 2 temas x 3 dificultades x cantidad=1 = 6 llamadas.
        verify(geminiClient, times(6)).generarPregunta(any(), any(), any());
        verify(preguntaRepository, times(6)).save(any());
    }

    @Test
    void generarTodosSaltaUnParQueYaTieneSuficientesPreguntas() {
        Tema tema = temaConId(1L, "Algebra");
        given(temaRepository.findAll()).willReturn(List.of(tema));
        given(preguntaRepository.countByTemaIdAndDificultad(1L, Dificultad.FACIL)).willReturn(5L);
        given(preguntaRepository.countByTemaIdAndDificultad(1L, Dificultad.MEDIA)).willReturn(0L);
        given(preguntaRepository.countByTemaIdAndDificultad(1L, Dificultad.DIFICIL)).willReturn(0L);
        given(geminiClient.generarPregunta(any(), any(), any())).willReturn(preguntaValida());
        given(validator.validar(any())).willReturn(Optional.empty());

        runner.generarTodos(5);

        // FACIL ya tiene 5 (>= cantidad): se salta por completo. MEDIA y DIFICIL generan 5 cada una.
        verify(geminiClient, never()).generarPregunta(any(), any(), eq(Dificultad.FACIL));
        verify(geminiClient, times(5)).generarPregunta(any(), any(), eq(Dificultad.MEDIA));
        verify(geminiClient, times(5)).generarPregunta(any(), any(), eq(Dificultad.DIFICIL));
        verify(preguntaRepository, times(10)).save(any());
    }

    @Test
    void generarTodosNoGuardaLasPreguntasRechazadasPorElValidador() {
        Tema tema = temaConId(1L, "Algebra");
        given(temaRepository.findAll()).willReturn(List.of(tema));
        given(preguntaRepository.countByTemaIdAndDificultad(any(), any())).willReturn(0L);
        given(geminiClient.generarPregunta(any(), any(), any())).willReturn(preguntaValida());
        given(validator.validar(any())).willReturn(Optional.of("explicacion vacia"));

        runner.generarTodos(1);

        verify(preguntaRepository, never()).save(any());
    }
}
