package com.rumbou.backend.contenido;

import com.rumbou.backend.academico.AreaConocimiento;
import com.rumbou.backend.academico.Tema;
import com.rumbou.backend.academico.TemaRepository;
import com.rumbou.backend.auth.Role;
import com.rumbou.backend.auth.Usuario;
import com.rumbou.backend.contenido.dto.PreguntaResponse;
import com.rumbou.backend.contenido.dto.TutorIaResponse;
import com.rumbou.backend.contenido.gemini.GeminiClient;
import com.rumbou.backend.contenido.gemini.GeminiException;
import com.rumbou.backend.shared.exception.ResourceNotFoundException;
import com.rumbou.backend.shared.exception.UnauthorizedException;
import com.rumbou.backend.suscripcion.Funcionalidad;
import com.rumbou.backend.suscripcion.PlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// JUnit + Mockito, sin contexto de Spring: aqui solo interesa probar la regla de
// negocio de visibilidad (postulante nunca ve una pregunta sin aprobar), no el
// cableado HTTP (eso ya lo cubre PreguntaControllerTest).
class PreguntaServiceTest {

    @Mock
    private PreguntaRepository preguntaRepository;

    @Mock
    private TemaRepository temaRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private PlanService planService;

    private PreguntaService preguntaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        preguntaService = new PreguntaService(preguntaRepository, temaRepository, geminiClient, planService);
    }

    private Pregunta preguntaConId(Long id, boolean aprobada) {
        Tema tema = new Tema("Algebra", AreaConocimiento.MATEMATICA);
        tema.setId(1L);
        Pregunta pregunta = new Pregunta(tema, "¿Cuanto es 2 + 2?",
                List.of("1", "2", "3", "4", "5"), 3, "2 + 2 = 4", Dificultad.FACIL, OrigenPregunta.SEMILLA, aprobada);
        pregunta.setId(id);
        return pregunta;
    }

    private Usuario usuarioConId(Long id) {
        Usuario usuario = new Usuario("postulante@rumbou.com", "hash", "Postulante", Role.USER);
        usuario.setId(id);
        return usuario;
    }

    @Test
    void unAdminPuedeVerUnaPreguntaSinAprobar() {
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(preguntaConId(1L, false)));

        PreguntaResponse respuesta = preguntaService.obtener(1L, true);

        assertThat(respuesta.id()).isEqualTo(1L);
    }

    @Test
    void unNoAdminNoPuedeVerUnaPreguntaSinAprobar() {
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(preguntaConId(1L, false)));

        assertThatThrownBy(() -> preguntaService.obtener(1L, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unNoAdminSiPuedeVerUnaPreguntaAprobada() {
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(preguntaConId(1L, true)));

        PreguntaResponse respuesta = preguntaService.obtener(1L, false);

        assertThat(respuesta.id()).isEqualTo(1L);
    }

    @Test
    void unNoAdminSiempreBuscaSoloAprobadasAunquePidaLoContrario() {
        given(preguntaRepository.buscar(any(), any(), any(), org.mockito.ArgumentMatchers.eq(Boolean.TRUE), any()))
                .willReturn(org.springframework.data.domain.Page.empty());

        preguntaService.buscar(null, null, null, Boolean.FALSE, org.springframework.data.domain.Pageable.unpaged(), false);

        org.mockito.Mockito.verify(preguntaRepository)
                .buscar(any(), any(), any(), org.mockito.ArgumentMatchers.eq(Boolean.TRUE), any());
    }

    @Test
    void unAdminBuscaConElFiltroQuePida() {
        given(preguntaRepository.buscar(any(), any(), any(), org.mockito.ArgumentMatchers.isNull(), any()))
                .willReturn(org.springframework.data.domain.Page.empty());

        preguntaService.buscar(null, null, null, null, org.springframework.data.domain.Pageable.unpaged(), true);

        org.mockito.Mockito.verify(preguntaRepository)
                .buscar(any(), any(), any(), org.mockito.ArgumentMatchers.isNull(), any());
    }

    @Test
    void aprobarMarcaLaPreguntaComoAprobada() {
        Pregunta pregunta = preguntaConId(1L, false);
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(pregunta));

        var respuesta = preguntaService.aprobar(1L);

        assertThat(respuesta.aprobada()).isTrue();
        assertThat(pregunta.isAprobada()).isTrue();
    }

    @Test
    void unUsuarioConCupoRecibeLaExplicacionYGastaUnaConsultaDelDia() {
        Pregunta pregunta = preguntaConId(1L, true);
        Usuario usuario = usuarioConId(10L);
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(pregunta));
        given(geminiClient.explicar(pregunta.getEnunciado(), pregunta.getAlternativas(), pregunta.getClaveCorrecta()))
                .willReturn("Porque 2 + 2 = 4");

        TutorIaResponse respuesta = preguntaService.pedirExplicacionTutorIa(1L, usuario, false);

        assertThat(respuesta.preguntaId()).isEqualTo(1L);
        assertThat(respuesta.explicacion()).isEqualTo("Porque 2 + 2 = 4");
        verify(planService).puedeAcceder(usuario, Funcionalidad.TUTOR_IA);
        verify(planService).registrarUso(usuario, Funcionalidad.TUTOR_IA);
    }

    @Test
    void siPlanServiceRechazaElAccesoNuncaSeLlamaAGemini() {
        Pregunta pregunta = preguntaConId(1L, true);
        Usuario usuario = usuarioConId(10L);
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(pregunta));
        willThrow(new UnauthorizedException("El tutor de IA es exclusivo del plan PRO"))
                .given(planService).puedeAcceder(usuario, Funcionalidad.TUTOR_IA);

        assertThatThrownBy(() -> preguntaService.pedirExplicacionTutorIa(1L, usuario, false))
                .isInstanceOf(UnauthorizedException.class);

        verify(geminiClient, never()).explicar(any(), any(), any(Integer.class));
        verify(planService, never()).registrarUso(any(), any());
    }

    @Test
    void siGeminiFallaNoSeDescuentaElCupoDelDia() {
        Pregunta pregunta = preguntaConId(1L, true);
        Usuario usuario = usuarioConId(10L);
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(pregunta));
        given(geminiClient.explicar(any(), any(), any(Integer.class)))
                .willThrow(new GeminiException("Gemini no respondio"));

        assertThatThrownBy(() -> preguntaService.pedirExplicacionTutorIa(1L, usuario, false))
                .isInstanceOf(GeminiException.class);

        verify(planService, never()).registrarUso(any(), any());
    }

    @Test
    void unNoAdminNoPuedePedirleAlTutorIaUnaPreguntaSinAprobar() {
        Pregunta pregunta = preguntaConId(1L, false);
        Usuario usuario = usuarioConId(10L);
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(pregunta));

        assertThatThrownBy(() -> preguntaService.pedirExplicacionTutorIa(1L, usuario, false))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(planService, never()).puedeAcceder(any(), any());
        verify(geminiClient, never()).explicar(any(), any(), any(Integer.class));
    }

    @Test
    void unAdminSiPuedePedirleAlTutorIaUnaPreguntaSinAprobar() {
        Pregunta pregunta = preguntaConId(1L, false);
        Usuario usuario = usuarioConId(10L);
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(pregunta));
        given(geminiClient.explicar(any(), any(), any(Integer.class))).willReturn("Explicacion");

        TutorIaResponse respuesta = preguntaService.pedirExplicacionTutorIa(1L, usuario, true);

        assertThat(respuesta.explicacion()).isEqualTo("Explicacion");
    }
}
