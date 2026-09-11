package com.rumbou.backend.contenido;

import com.rumbou.backend.academico.AreaConocimiento;
import com.rumbou.backend.academico.Tema;
import com.rumbou.backend.contenido.dto.PreguntaResponse;
import com.rumbou.backend.shared.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
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

// JUnit + Mockito, sin contexto de Spring: aqui solo interesa probar la regla de
// negocio de visibilidad (postulante nunca ve una pregunta sin aprobar), no el
// cableado HTTP (eso ya lo cubre PreguntaControllerTest).
class PreguntaServiceTest {

    @Mock
    private PreguntaRepository preguntaRepository;

    @Mock
    private EntityManager entityManager;

    private PreguntaService preguntaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        preguntaService = new PreguntaService(preguntaRepository, entityManager);
    }

    private Pregunta preguntaConId(Long id, boolean aprobada) {
        Tema tema = new Tema("Algebra", AreaConocimiento.MATEMATICA);
        tema.setId(1L);
        Pregunta pregunta = new Pregunta(tema, "¿Cuanto es 2 + 2?",
                List.of("1", "2", "3", "4", "5"), 3, "2 + 2 = 4", Dificultad.FACIL, OrigenPregunta.SEMILLA, aprobada);
        pregunta.setId(id);
        return pregunta;
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
}
