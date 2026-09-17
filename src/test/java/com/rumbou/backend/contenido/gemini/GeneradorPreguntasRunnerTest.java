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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// JUnit + Mockito puro: prueba el recorrido de --todos, el salteo idempotente y
// la tolerancia a fallos por par, sin llamar a Gemini de verdad.
// pausaEntreLlamadasMs=0 para que el test no espere.
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

    // El validador en lote devuelve un Optional por pregunta, en el mismo orden:
    // aqui acepta todas. Los tests que rechazan lo redefinen.
    private void validadorAceptaTodo() {
        given(validator.validarLote(any())).willAnswer(inv -> {
            List<PreguntaGeneradaDto> lote = inv.getArgument(0);
            return lote.stream().map(p -> Optional.<String>empty()).toList();
        });
    }

    private List<PreguntaGeneradaDto> preguntasValidas(int cantidad) {
        List<PreguntaGeneradaDto> preguntas = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            preguntas.add(preguntaValida());
        }
        return preguntas;
    }

    @Test
    void generarTodosRecorreTodosLosTemasYLasTresDificultadesConUnaLlamadaPorPar() {
        Tema tema1 = temaConId(1L, "Algebra");
        Tema tema2 = temaConId(2L, "Geometria");
        given(temaRepository.findAll()).willReturn(List.of(tema1, tema2));
        given(preguntaRepository.countByTemaIdAndDificultad(any(), any())).willReturn(0L);
        given(geminiClient.generarPreguntas(any(), any(), any(), anyInt())).willReturn(List.of(preguntaValida()));
        validadorAceptaTodo();

        runner.generarTodos(1, true);

        // 2 temas x 3 dificultades = 6 pares: una llamada de generacion y una de
        // validacion por par (no una por pregunta).
        verify(geminiClient, times(6)).generarPreguntas(any(), any(), any(), eq(1));
        verify(validator, times(6)).validarLote(any());
        verify(preguntaRepository, times(6)).save(any());
    }

    @Test
    void sinValidarSoloRevisaLaEstructuraYNoLlamaAlValidadorConIa() {
        Tema tema = temaConId(1L, "Algebra");
        given(temaRepository.findAll()).willReturn(List.of(tema));
        given(preguntaRepository.countByTemaIdAndDificultad(any(), any())).willReturn(0L);
        given(geminiClient.generarPreguntas(any(), any(), any(), anyInt())).willReturn(preguntasValidas(2));
        given(validator.validarEstructura(any())).willReturn(Optional.empty());

        runner.generarTodos(2, false);

        verify(validator, never()).validarLote(any());
        verify(validator, times(6)).validarEstructura(any());
        verify(preguntaRepository, times(6)).save(any());
    }

    @Test
    void siLaValidacionConIaFallaElLoteSeGuardaConValidacionEstructural() {
        Tema tema = temaConId(1L, "Algebra");
        given(temaRepository.findAll()).willReturn(List.of(tema));
        given(preguntaRepository.countByTemaIdAndDificultad(any(), any())).willReturn(0L);
        given(geminiClient.generarPreguntas(any(), any(), any(), anyInt())).willReturn(preguntasValidas(2));
        given(validator.validarLote(any())).willThrow(new GeminiException("Gemini sigue respondiendo 429"));
        given(validator.validarEstructura(any())).willReturn(Optional.empty());

        runner.generarTodos(2, true);

        // La cuota agotada en la validacion no tira las preguntas ya generadas
        // (que costaron su propia llamada): quedan para la revision humana.
        verify(preguntaRepository, times(6)).save(any());
    }

    @Test
    void generarTodosSaltaUnParQueYaTieneSuficientesPreguntas() {
        Tema tema = temaConId(1L, "Algebra");
        given(temaRepository.findAll()).willReturn(List.of(tema));
        given(preguntaRepository.countByTemaIdAndDificultad(1L, Dificultad.FACIL)).willReturn(5L);
        given(preguntaRepository.countByTemaIdAndDificultad(1L, Dificultad.MEDIA)).willReturn(0L);
        given(preguntaRepository.countByTemaIdAndDificultad(1L, Dificultad.DIFICIL)).willReturn(0L);
        given(geminiClient.generarPreguntas(any(), any(), any(), eq(5))).willReturn(preguntasValidas(5));
        validadorAceptaTodo();

        runner.generarTodos(5, true);

        // FACIL ya tiene 5 (>= cantidad): se salta por completo, ni una llamada.
        verify(geminiClient, never()).generarPreguntas(any(), any(), eq(Dificultad.FACIL), anyInt());
        verify(geminiClient, times(1)).generarPreguntas(any(), any(), eq(Dificultad.MEDIA), eq(5));
        verify(geminiClient, times(1)).generarPreguntas(any(), any(), eq(Dificultad.DIFICIL), eq(5));
        verify(preguntaRepository, times(10)).save(any());
    }

    @Test
    void generarTodosNoGuardaLasPreguntasRechazadasPorElValidador() {
        Tema tema = temaConId(1L, "Algebra");
        given(temaRepository.findAll()).willReturn(List.of(tema));
        given(preguntaRepository.countByTemaIdAndDificultad(any(), any())).willReturn(0L);
        given(geminiClient.generarPreguntas(any(), any(), any(), anyInt())).willReturn(List.of(preguntaValida()));
        given(validator.validarLote(any())).willReturn(List.of(Optional.of("explicacion vacia")));

        runner.generarTodos(1, true);

        verify(preguntaRepository, never()).save(any());
    }

    @Test
    void generarTodosSigueConLosDemasParesSiUnoFallaPorErrorDeGemini() {
        Tema tema1 = temaConId(1L, "Algebra");
        Tema tema2 = temaConId(2L, "Geometria");
        given(temaRepository.findAll()).willReturn(List.of(tema1, tema2));
        given(preguntaRepository.countByTemaIdAndDificultad(any(), any())).willReturn(0L);
        given(geminiClient.generarPreguntas(eq("Algebra"), any(), any(), anyInt()))
                .willThrow(new GeminiException("Gemini respondio 500"));
        given(geminiClient.generarPreguntas(eq("Geometria"), any(), any(), anyInt()))
                .willReturn(List.of(preguntaValida()));
        validadorAceptaTodo();

        runner.generarTodos(1, true);

        // Algebra fallo en sus 3 pares, pero Geometria se sigue procesando igual.
        verify(preguntaRepository, times(3)).save(any());
    }
}
