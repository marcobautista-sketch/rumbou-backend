package com.rumbou.backend.service;

import com.rumbou.backend.client.gemini.GeminiClient;
import com.rumbou.backend.client.gemini.GeminiException;
import com.rumbou.backend.client.gemini.RevisionSolucionDto;
import com.rumbou.backend.dto.response.RevisionSolucionResponse;
import com.rumbou.backend.entity.AreaConocimiento;
import com.rumbou.backend.entity.Dificultad;
import com.rumbou.backend.entity.Funcionalidad;
import com.rumbou.backend.entity.OrigenPregunta;
import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.entity.VeredictoSolucion;
import com.rumbou.backend.exception.ForbiddenException;
import com.rumbou.backend.exception.InvalidOperationException;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.repository.PreguntaRepository;
import com.rumbou.backend.security.CurrentUserService;
import com.rumbou.backend.service.impl.RevisionSolucionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RevisionSolucionServiceTest {

    private static final byte[] FOTO = {1, 2, 3};

    @Mock
    private PreguntaRepository preguntaRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private PlanService planService;

    @Mock
    private CurrentUserService currentUserService;

    private RevisionSolucionServiceImpl revisionSolucionService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        revisionSolucionService = new RevisionSolucionServiceImpl(preguntaRepository, geminiClient, planService,
                currentUserService);
        usuario = new Usuario("postulante@rumbou.com", "hash", "Postulante", Role.USER);
        usuario.setId(10L);
        given(currentUserService.getUsuario()).willReturn(usuario);
    }

    private Pregunta pregunta(boolean aprobada) {
        Tema tema = new Tema("Algebra", AreaConocimiento.MATEMATICA);
        tema.setId(1L);
        Pregunta pregunta = new Pregunta(tema, "¿Cuanto es 2 + 2?",
                List.of("1", "2", "3", "4", "5"), 3, "2 + 2 = 4", Dificultad.FACIL, OrigenPregunta.SEMILLA, aprobada);
        pregunta.setId(1L);
        return pregunta;
    }

    private MockMultipartFile foto(String tipo, byte[] contenido) {
        return new MockMultipartFile("imagen", "solucion.jpg", tipo, contenido);
    }

    @Test
    void geminiRevisaLaFotoConLaClaveYSeGastaUnaConsultaDelDia() {
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(pregunta(true)));
        given(geminiClient.revisarSolucion("¿Cuanto es 2 + 2?", List.of("1", "2", "3", "4", "5"), 3, "2 + 2 = 4",
                FOTO, "image/jpeg"))
                .willReturn(new RevisionSolucionDto(VeredictoSolucion.PARCIAL, "Sumaste bien, pero marcaste la 2"));

        RevisionSolucionResponse respuesta = revisionSolucionService.revisar(1L, foto("image/jpeg", FOTO));

        assertThat(respuesta.preguntaId()).isEqualTo(1L);
        assertThat(respuesta.veredicto()).isEqualTo(VeredictoSolucion.PARCIAL);
        assertThat(respuesta.retroalimentacion()).isEqualTo("Sumaste bien, pero marcaste la 2");
        verify(planService).puedeAcceder(usuario, Funcionalidad.TUTOR_IA);
        verify(planService).registrarUso(usuario, Funcionalidad.TUTOR_IA);
    }

    @Test
    void elTipoDeLaImagenSeNormalizaAMinusculas() {
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(pregunta(true)));
        given(geminiClient.revisarSolucion(any(), any(), anyInt(), any(), any(), eq("image/png")))
                .willReturn(new RevisionSolucionDto(VeredictoSolucion.CORRECTA, "Bien resuelto"));

        RevisionSolucionResponse respuesta = revisionSolucionService.revisar(1L, foto("IMAGE/PNG", FOTO));

        assertThat(respuesta.veredicto()).isEqualTo(VeredictoSolucion.CORRECTA);
    }

    @Test
    void unArchivoQueNoEsImagenSeRechazaSinLlamarAGemini() {
        assertThatThrownBy(() -> revisionSolucionService.revisar(1L, foto("application/pdf", FOTO)))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("JPG, PNG, WEBP o HEIC");

        verifyNoInteractions(geminiClient, planService);
    }

    @Test
    void unaImagenVaciaSeRechaza() {
        assertThatThrownBy(() -> revisionSolucionService.revisar(1L, foto("image/jpeg", new byte[0])))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("La imagen esta vacia");

        verifyNoInteractions(geminiClient);
    }

    @Test
    void unPostulanteNoPuedeRevisarUnaPreguntaSinAprobar() {
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(pregunta(false)));
        given(currentUserService.puedeRevisarPreguntas()).willReturn(false);

        assertThatThrownBy(() -> revisionSolucionService.revisar(1L, foto("image/jpeg", FOTO)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(geminiClient);
    }

    @Test
    void sinPlanProNuncaSeLlamaAGemini() {
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(pregunta(true)));
        willThrow(new ForbiddenException("El tutor de IA es exclusivo del plan PRO"))
                .given(planService).puedeAcceder(usuario, Funcionalidad.TUTOR_IA);

        assertThatThrownBy(() -> revisionSolucionService.revisar(1L, foto("image/jpeg", FOTO)))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(geminiClient);
        verify(planService, never()).registrarUso(any(), any());
    }

    @Test
    void siGeminiFallaNoSeDescuentaElCupoDelDia() {
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(pregunta(true)));
        given(geminiClient.revisarSolucion(any(), any(), anyInt(), any(), any(), any()))
                .willThrow(new GeminiException("Gemini no respondio"));

        assertThatThrownBy(() -> revisionSolucionService.revisar(1L, foto("image/jpeg", FOTO)))
                .isInstanceOf(GeminiException.class);

        verify(planService, never()).registrarUso(any(), any());
    }
}
