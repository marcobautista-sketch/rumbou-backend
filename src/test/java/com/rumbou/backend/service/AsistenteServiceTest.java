package com.rumbou.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumbou.backend.client.gemini.GeminiClient;
import com.rumbou.backend.client.gemini.GeminiException;
import com.rumbou.backend.client.gemini.RespuestaAsistenteDto;
import com.rumbou.backend.dto.request.MensajePrevioRequest;
import com.rumbou.backend.dto.response.AreaResponse;
import com.rumbou.backend.dto.response.AsistenteResponse;
import com.rumbou.backend.dto.response.DominioTemaResponse;
import com.rumbou.backend.dto.response.EstadoPreparacion;
import com.rumbou.backend.dto.response.GamificacionResponse;
import com.rumbou.backend.dto.response.ObjetivoResponse;
import com.rumbou.backend.entity.Funcionalidad;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.RolMensaje;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.ForbiddenException;
import com.rumbou.backend.exception.InvalidOperationException;
import com.rumbou.backend.exception.PlanLimitExceededException;
import com.rumbou.backend.security.CurrentUserService;
import com.rumbou.backend.service.impl.AsistenteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AsistenteServiceTest {

    private static final byte[] AUDIO = {1, 2, 3};

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private PlanService planService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ObjetivoService objetivoService;

    @Mock
    private AreaService areaService;

    @Mock
    private ProgresoService progresoService;

    @Mock
    private GamificacionService gamificacionService;

    private AsistenteServiceImpl asistenteService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        asistenteService = new AsistenteServiceImpl(geminiClient, planService, currentUserService, objetivoService,
                areaService, progresoService, gamificacionService, new ObjectMapper());
        usuario = new Usuario("ana@rumbou.com", "hash", "Ana", Role.USER);
        usuario.setId(10L);
        given(currentUserService.getUsuario()).willReturn(usuario);
        given(gamificacionService.obtenerResumen())
                .willReturn(new GamificacionResponse(900, 120, 4, 6, LocalDate.now(), List.of()));
        given(areaService.listar()).willReturn(List.of(new AreaResponse(3L, "UNI", "A", "Ingenieria",
                "UNI 2027-1", LocalDate.of(2027, 2, 15), false)));
        given(objetivoService.listarObjetivos()).willReturn(List.of(new ObjetivoResponse(1L, 5L, "UNI", 3L,
                "Ingenieria", "Ingenieria de Sistemas", "2026-II", 13.5, 12.2, 0.9037, EstadoPreparacion.CERCA,
                "Cerca, falta poco", null)));
        given(progresoService.dominioPorTema()).willReturn(List.of(
                new DominioTemaResponse(1L, "Álgebra", 2, 8, 0, 10, 20.0),
                new DominioTemaResponse(2L, "Física", 5, 5, 0, 10, 50.0),
                new DominioTemaResponse(3L, "Aritmética", 9, 1, 0, 10, 90.0)));
        given(geminiClient.asistir(anyString(), anyString(), any(), any(), any()))
                .willReturn(new RespuestaAsistenteDto("¿Cuando es mi examen?", "Tu examen es el 15 de febrero."));
    }

    private MockMultipartFile audio(String tipo, byte[] contenido) {
        return new MockMultipartFile("audio", "pregunta.m4a", tipo, contenido);
    }

    @Test
    void elAsistenteRecibeElProgresoRealDelPostulante() {
        ArgumentCaptor<String> datos = ArgumentCaptor.forClass(String.class);

        AsistenteResponse respuesta = asistenteService.responderTexto("  ¿Cuando es mi examen?  ", List.of());

        verify(geminiClient).asistir(datos.capture(), eq(""), eq("¿Cuando es mi examen?"), isNull(), isNull());
        assertThat(datos.getValue())
                .contains("Nombre: Ana")
                .contains("Racha actual: 4 dias")
                .contains("Ingenieria de Sistemas en la UNI")
                .contains("indice de preparacion: 0.9 (Cerca, falta poco)")
                .contains("proximo examen: UNI 2027-1 el 2027-02-15 (fecha estimada)")
                .contains("Temas mas debiles: Álgebra (20% de aciertos en 10 preguntas)")
                .contains("Temas mas fuertes: Aritmética (90%");
        assertThat(respuesta.transcripcion()).isEqualTo("¿Cuando es mi examen?");
        assertThat(respuesta.respuesta()).isEqualTo("Tu examen es el 15 de febrero.");
        verify(planService).registrarUso(usuario, Funcionalidad.TUTOR_IA);
    }

    @Test
    void soloSeEnvianLosUltimosSeisMensajesDeLaConversacion() {
        List<MensajePrevioRequest> historial = IntStream.rangeClosed(1, 8)
                .mapToObj(i -> new MensajePrevioRequest(i % 2 == 1 ? RolMensaje.POSTULANTE : RolMensaje.ASISTENTE,
                        "mensaje " + i))
                .toList();
        ArgumentCaptor<String> conversacion = ArgumentCaptor.forClass(String.class);

        asistenteService.responderTexto("¿Y ahora?", historial);

        verify(geminiClient).asistir(anyString(), conversacion.capture(), anyString(), isNull(), isNull());
        assertThat(conversacion.getValue()).startsWith("Postulante: mensaje 3").endsWith("Asistente: mensaje 8")
                .doesNotContain("mensaje 2");
    }

    @Test
    void laVozLlegaConSuTipoNormalizadoYDevuelveLaTranscripcion() {
        AsistenteResponse respuesta = asistenteService.responderVoz(audio("audio/x-m4a", AUDIO),
                "[{\"rol\":\"POSTULANTE\",\"texto\":\"Hola\"},{\"rol\":\"ASISTENTE\",\"texto\":\"Hola, Ana\"}]");

        verify(geminiClient).asistir(anyString(), eq("Postulante: Hola\nAsistente: Hola, Ana"), isNull(),
                eq(AUDIO), eq("audio/mp4"));
        assertThat(respuesta.transcripcion()).isEqualTo("¿Cuando es mi examen?");
    }

    @Test
    void elAudioDelNavegadorConCodecTambienSeAcepta() {
        asistenteService.responderVoz(audio("audio/webm;codecs=opus", AUDIO), null);

        verify(geminiClient).asistir(anyString(), eq(""), isNull(), eq(AUDIO), eq("audio/webm"));
    }

    @Test
    void unArchivoQueNoEsAudioSeRechazaSinLlamarAGemini() {
        assertThatThrownBy(() -> asistenteService.responderVoz(audio("image/png", AUDIO), null))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("WAV, M4A");

        verifyNoInteractions(geminiClient, planService);
    }

    @Test
    void unHistorialQueNoEsJsonSeRechaza() {
        assertThatThrownBy(() -> asistenteService.responderVoz(audio("audio/wav", AUDIO), "no es json"))
                .isInstanceOf(InvalidOperationException.class);

        verifyNoInteractions(geminiClient);
    }

    @Test
    void sinPlanProNuncaSeLlamaAGemini() {
        willThrow(new ForbiddenException("El tutor de IA es exclusivo del plan PRO"))
                .given(planService).puedeAcceder(usuario, Funcionalidad.TUTOR_IA);

        assertThatThrownBy(() -> asistenteService.responderTexto("Hola", List.of()))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(geminiClient);
    }

    @Test
    void siGeminiFallaNoSeDescuentaElCupoDelDia() {
        given(geminiClient.asistir(anyString(), anyString(), any(), any(), any()))
                .willThrow(new GeminiException("Gemini no respondio"));

        assertThatThrownBy(() -> asistenteService.responderTexto("Hola", List.of()))
                .isInstanceOf(GeminiException.class);

        verify(planService, never()).registrarUso(any(), any());
    }

    @Test
    void sinDominioPorTemaElAsistenteIgualResponde() {
        given(progresoService.dominioPorTema()).willThrow(new PlanLimitExceededException("Solo PRO"));
        ArgumentCaptor<String> datos = ArgumentCaptor.forClass(String.class);

        asistenteService.responderTexto("Hola", List.of());

        verify(geminiClient).asistir(datos.capture(), anyString(), anyString(), isNull(), isNull());
        assertThat(datos.getValue()).contains("Nombre: Ana").doesNotContain("Temas mas debiles");
    }
}
