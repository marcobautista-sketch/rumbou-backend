package com.rumbou.backend.client.gemini;

import com.rumbou.backend.entity.AreaConocimiento;
import com.rumbou.backend.entity.Dificultad;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.repository.PreguntaRepository;
import com.rumbou.backend.repository.TemaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// JUnit + Mockito puro: prueba el recorrido de --todos (una llamada por tema),
// el salteo idempotente por par y la tolerancia a fallos, sin llamar a Gemini
// de verdad. pausaEntreLlamadasMs=0 para que el test no espere.
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

    private List<PreguntaGeneradaDto> preguntasValidas(int cantidad) {
        List<PreguntaGeneradaDto> preguntas = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            preguntas.add(preguntaValida());
        }
        return preguntas;
    }

    // Lo que devuelve Gemini en la llamada por tema: "cantidad" preguntas por
    // cada una de las tres dificultades.
    private Map<Dificultad, List<PreguntaGeneradaDto>> lotePorTema(int cantidadPorDificultad) {
        Map<Dificultad, List<PreguntaGeneradaDto>> lote = new EnumMap<>(Dificultad.class);
        for (Dificultad dificultad : Dificultad.values()) {
            lote.put(dificultad, preguntasValidas(cantidadPorDificultad));
        }
        return lote;
    }

    // El validador en lote devuelve un Optional por pregunta, en el mismo orden:
    // aqui acepta todas. Los tests que rechazan lo redefinen.
    private void validadorAceptaTodo() {
        given(validator.validarLote(any())).willAnswer(inv -> {
            List<PreguntaGeneradaDto> lote = inv.getArgument(0);
            return lote.stream().map(p -> Optional.<String>empty()).toList();
        });
    }

    @Test
    void generarTodosHaceUnaSolaLlamadaDeGeneracionPorTema() {
        Tema tema1 = temaConId(1L, "Algebra");
        Tema tema2 = temaConId(2L, "Geometria");
        given(temaRepository.findAll()).willReturn(List.of(tema1, tema2));
        given(preguntaRepository.countByTemaIdAndDificultad(any(), any())).willReturn(0L);
        given(geminiClient.generarPreguntasPorTema(any(), any(), any())).willReturn(lotePorTema(1));
        validadorAceptaTodo();

        runner.generarTodos(1, true);

        // 2 temas = 2 llamadas de generacion (no 6, una por par) y una de
        // validacion por dificultad recibida.
        verify(geminiClient, times(2)).generarPreguntasPorTema(any(), any(), any());
        verify(validator, times(6)).validarLote(any());
        verify(preguntaRepository, times(6)).save(any());
    }

    @Test
    void generarTodosPideSoloLasDificultadesQueFaltanYSaltaElTemaCompleto() {
        Tema algebra = temaConId(1L, "Algebra");
        Tema geometria = temaConId(2L, "Geometria");
        given(temaRepository.findAll()).willReturn(List.of(algebra, geometria));
        // Algebra: FACIL ya completa, MEDIA a medias (2 de 5), DIFICIL vacia.
        given(preguntaRepository.countByTemaIdAndDificultad(1L, Dificultad.FACIL)).willReturn(5L);
        given(preguntaRepository.countByTemaIdAndDificultad(1L, Dificultad.MEDIA)).willReturn(2L);
        given(preguntaRepository.countByTemaIdAndDificultad(1L, Dificultad.DIFICIL)).willReturn(0L);
        // Geometria: todo completo.
        given(preguntaRepository.countByTemaIdAndDificultad(eq(2L), any())).willReturn(5L);
        Map<Dificultad, List<PreguntaGeneradaDto>> respuesta = new EnumMap<>(Dificultad.class);
        respuesta.put(Dificultad.MEDIA, preguntasValidas(3));
        respuesta.put(Dificultad.DIFICIL, preguntasValidas(5));
        given(geminiClient.generarPreguntasPorTema(eq("Algebra"), any(), any())).willReturn(respuesta);
        validadorAceptaTodo();

        runner.generarTodos(5, true);

        // Geometria no gasta ninguna llamada; Algebra pide exactamente lo que falta.
        verify(geminiClient, never()).generarPreguntasPorTema(eq("Geometria"), any(), any());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Dificultad, Integer>> faltantes = ArgumentCaptor.forClass(Map.class);
        verify(geminiClient).generarPreguntasPorTema(eq("Algebra"), any(), faltantes.capture());
        assertThat(faltantes.getValue())
                .containsEntry(Dificultad.MEDIA, 3)
                .containsEntry(Dificultad.DIFICIL, 5)
                .doesNotContainKey(Dificultad.FACIL);
        verify(preguntaRepository, times(8)).save(any());
    }

    @Test
    void generarTodosNoGuardaLasPreguntasRechazadasPorElValidador() {
        Tema tema = temaConId(1L, "Algebra");
        given(temaRepository.findAll()).willReturn(List.of(tema));
        given(preguntaRepository.countByTemaIdAndDificultad(any(), any())).willReturn(0L);
        given(geminiClient.generarPreguntasPorTema(any(), any(), any())).willReturn(lotePorTema(1));
        given(validator.validarLote(any())).willReturn(List.of(Optional.of("explicacion vacia")));

        runner.generarTodos(1, true);

        verify(preguntaRepository, never()).save(any());
    }

    @Test
    void generarTodosSigueConLosDemasTemasSiUnoFallaPorErrorDeGemini() {
        Tema tema1 = temaConId(1L, "Algebra");
        Tema tema2 = temaConId(2L, "Geometria");
        given(temaRepository.findAll()).willReturn(List.of(tema1, tema2));
        given(preguntaRepository.countByTemaIdAndDificultad(any(), any())).willReturn(0L);
        given(geminiClient.generarPreguntasPorTema(eq("Algebra"), any(), any()))
                .willThrow(new GeminiException("Gemini respondio 500"));
        given(geminiClient.generarPreguntasPorTema(eq("Geometria"), any(), any())).willReturn(lotePorTema(1));
        validadorAceptaTodo();

        runner.generarTodos(1, true);

        // Algebra fallo, pero Geometria se sigue procesando igual (3 dificultades x 1).
        verify(preguntaRepository, times(3)).save(any());
    }

    @Test
    void sinValidarSoloRevisaLaEstructuraYNoLlamaAlValidadorConIa() {
        Tema tema = temaConId(1L, "Algebra");
        given(temaRepository.findAll()).willReturn(List.of(tema));
        given(preguntaRepository.countByTemaIdAndDificultad(any(), any())).willReturn(0L);
        given(geminiClient.generarPreguntasPorTema(any(), any(), any())).willReturn(lotePorTema(2));
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
        given(geminiClient.generarPreguntasPorTema(any(), any(), any())).willReturn(lotePorTema(2));
        given(validator.validarLote(any())).willThrow(new GeminiException("Gemini sigue respondiendo 429"));
        given(validator.validarEstructura(any())).willReturn(Optional.empty());

        runner.generarTodos(2, true);

        // La cuota agotada en la validacion no tira las preguntas ya generadas
        // (que costaron su propia llamada): quedan para la revision humana.
        verify(preguntaRepository, times(6)).save(any());
    }
}
