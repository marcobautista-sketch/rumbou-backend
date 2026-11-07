package com.rumbou.backend.service;

import com.rumbou.backend.dto.request.AprobarLoteRequest;
import com.rumbou.backend.dto.request.UpdatePreguntaRequest;
import com.rumbou.backend.dto.response.PreguntaAdminResponse;
import com.rumbou.backend.dto.response.PreguntaResponse;
import com.rumbou.backend.entity.AreaConocimiento;
import com.rumbou.backend.entity.Dificultad;
import com.rumbou.backend.entity.OrigenPregunta;
import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.exception.ForbiddenException;
import com.rumbou.backend.exception.ResourceInUseException;
import com.rumbou.backend.repository.PreguntaRepository;
import com.rumbou.backend.repository.RespuestaUsuarioRepository;
import com.rumbou.backend.security.CurrentUserService;
import com.rumbou.backend.repository.TemaRepository;
import com.rumbou.backend.service.impl.PreguntaServiceImpl;
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

// Solo la regla de visibilidad (un postulante nunca ve una pregunta sin aprobar); el HTTP lo cubre PreguntaControllerTest.
class PreguntaServiceTest {

    @Mock
    private PreguntaRepository preguntaRepository;

    @Mock
    private TemaRepository temaRepository;

    @Mock
    private RespuestaUsuarioRepository respuestaUsuarioRepository;

    @Mock
    private CurrentUserService currentUserService;

    private PreguntaServiceImpl preguntaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        preguntaService = new PreguntaServiceImpl(preguntaRepository, temaRepository, respuestaUsuarioRepository,
                currentUserService);
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
        given(currentUserService.puedeRevisarPreguntas()).willReturn(true);
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(preguntaConId(1L, false)));

        PreguntaResponse respuesta = preguntaService.obtener(1L);

        assertThat(respuesta.id()).isEqualTo(1L);
    }

    @Test
    void unNoAdminNoPuedeVerUnaPreguntaSinAprobar() {
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(preguntaConId(1L, false)));

        assertThatThrownBy(() -> preguntaService.obtener(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unNoAdminSiPuedeVerUnaPreguntaAprobada() {
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(preguntaConId(1L, true)));

        PreguntaResponse respuesta = preguntaService.obtener(1L);

        assertThat(respuesta.id()).isEqualTo(1L);
    }

    @Test
    void unNoAdminSiempreBuscaSoloAprobadasAunquePidaLoContrario() {
        given(preguntaRepository.buscar(any(), any(), org.mockito.ArgumentMatchers.eq(Boolean.TRUE), any(), any()))
                .willReturn(org.springframework.data.domain.Page.empty());

        preguntaService.buscar(null, null, Boolean.FALSE, null, org.springframework.data.domain.Pageable.unpaged());

        org.mockito.Mockito.verify(preguntaRepository)
                .buscar(any(), any(), org.mockito.ArgumentMatchers.eq(Boolean.TRUE), any(), any());
    }

    @Test
    void unAdminBuscaConElFiltroQuePida() {
        given(currentUserService.puedeRevisarPreguntas()).willReturn(true);
        given(preguntaRepository.buscar(any(), any(), org.mockito.ArgumentMatchers.isNull(), any(), any()))
                .willReturn(org.springframework.data.domain.Page.empty());

        preguntaService.buscar(null, null, null, null, org.springframework.data.domain.Pageable.unpaged());

        org.mockito.Mockito.verify(preguntaRepository)
                .buscar(any(), any(), org.mockito.ArgumentMatchers.isNull(), any(), any());
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
    void aprobarLoteMarcaTodasLasPreguntasDelLoteComoAprobadas() {
        Pregunta pregunta1 = preguntaConId(1L, false);
        Pregunta pregunta2 = preguntaConId(2L, false);
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(pregunta1));
        given(preguntaRepository.findById(2L)).willReturn(Optional.of(pregunta2));

        List<PreguntaAdminResponse> respuesta = preguntaService.aprobarLote(new AprobarLoteRequest(List.of(1L, 2L)));

        assertThat(respuesta).hasSize(2);
        assertThat(pregunta1.isAprobada()).isTrue();
        assertThat(pregunta2.isAprobada()).isTrue();
    }

    // El rollback lo hace @Transactional en produccion; aqui solo se comprueba que un id inexistente corta el lote.
    @Test
    void aprobarLoteFallaSiAlgunIdNoExiste() {
        Pregunta pregunta1 = preguntaConId(1L, false);
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(pregunta1));
        given(preguntaRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> preguntaService.aprobarLote(new AprobarLoteRequest(List.of(1L, 99L))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void noSeEliminaUnaPreguntaQueYaSalioEnUnSimulacro() {
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(preguntaConId(1L, true)));
        given(respuestaUsuarioRepository.existsByPreguntaId(1L)).willReturn(true);

        assertThatThrownBy(() -> preguntaService.eliminar(1L))
                .isInstanceOf(ResourceInUseException.class);

        verify(preguntaRepository, never()).delete(any());
    }

    @Test
    void seEliminaUnaPreguntaQueNuncaSeUso() {
        Pregunta pregunta = preguntaConId(1L, true);
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(pregunta));
        given(respuestaUsuarioRepository.existsByPreguntaId(1L)).willReturn(false);

        preguntaService.eliminar(1L);

        verify(preguntaRepository).delete(pregunta);
    }

    @Test
    void unRevisorVeLasPreguntasSinAprobarQueDebeRevisar() {
        given(currentUserService.puedeRevisarPreguntas()).willReturn(true);
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(preguntaConId(1L, false)));

        PreguntaResponse respuesta = preguntaService.obtener(1L);

        assertThat(respuesta.id()).isEqualTo(1L);
    }

    private Pregunta preguntaConPresentacion() {
        Pregunta pregunta = preguntaConId(1L, true);
        pregunta.setPresentacion("{\"enunciado\":\"¿Cuanto es $2 + 2$?\",\"alternativas\":null,\"figura\":null,"
                + "\"pasos\":[{\"titulo\":\"Suma\",\"detalle\":\"$2 + 2 = 4$\",\"tabla\":[]}]}");
        given(preguntaRepository.findById(1L)).willReturn(Optional.of(pregunta));
        given(temaRepository.findById(1L)).willReturn(Optional.of(pregunta.getTema()));
        return pregunta;
    }

    @Test
    void editarElTextoDeLaPreguntaQuitaSuVersionConFormato() {
        Pregunta pregunta = preguntaConPresentacion();

        preguntaService.actualizar(1L, new UpdatePreguntaRequest(1L, "¿Cuanto es 2 + 3?",
                List.of("1", "2", "3", "4", "5"), 4, "2 + 3 = 5", Dificultad.FACIL, true));

        assertThat(pregunta.getPresentacion()).isNull();
    }

    @Test
    void editarSoloLaDificultadConservaLaVersionConFormato() {
        Pregunta pregunta = preguntaConPresentacion();

        PreguntaAdminResponse respuesta = preguntaService.actualizar(1L, new UpdatePreguntaRequest(1L,
                "¿Cuanto es 2 + 2?", List.of("1", "2", "3", "4", "5"), 3, "2 + 2 = 4",
                Dificultad.MEDIA, true));

        assertThat(pregunta.getPresentacion()).isNotNull();
        assertThat(respuesta.pasos()).hasSize(1);
    }
}
