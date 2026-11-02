package com.rumbou.backend.mapper;

import com.rumbou.backend.dto.response.PresentacionResponse;
import com.rumbou.backend.dto.response.RespuestaDetalleResponse;
import com.rumbou.backend.dto.response.SimulacroDetalleResponse;
import com.rumbou.backend.dto.response.SimulacroResponse;
import com.rumbou.backend.entity.Area;
import com.rumbou.backend.entity.AreaConocimiento;
import com.rumbou.backend.entity.Dificultad;
import com.rumbou.backend.entity.EstadoSimulacro;
import com.rumbou.backend.entity.OrigenPregunta;
import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.entity.RespuestaUsuario;
import com.rumbou.backend.entity.Simulacro;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.entity.TipoSimulacro;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// La resolucion revela la respuesta: el problema con formato se ve siempre, los pasos solo
// cuando ya se revela la explicacion. Y un JSON danado no tumba la pregunta.
class PresentacionMapperTest {

    private static final String PRESENTACION = """
            {"enunciado": "Halle $x$ si $2x = 4$.",
             "alternativas": ["$1$", "$2$", "$3$", "$4$", "$5$"],
             "figura": null,
             "pasos": [{"titulo": "Despeja x", "detalle": "$$x = \\\\dfrac{4}{2} = 2$$", "tabla": []}]}
            """;

    private Pregunta pregunta(String presentacion) {
        Tema tema = new Tema("Algebra", AreaConocimiento.MATEMATICA);
        tema.setId(1L);
        Pregunta pregunta = new Pregunta(tema, "Halle x si 2x = 4.", List.of("1", "2", "3", "4", "5"), 1,
                "x = 4/2 = 2", Dificultad.FACIL, OrigenPregunta.SEMILLA, true);
        pregunta.setId(10L);
        pregunta.setPresentacion(presentacion);
        return pregunta;
    }

    private Simulacro simulacro(EstadoSimulacro estado) {
        Area area = new Area();
        area.setId(1L);
        Simulacro simulacro = new Simulacro(null, area, TipoSimulacro.POR_TEMA, LocalDateTime.now());
        simulacro.setEstado(estado);
        return simulacro;
    }

    private RespuestaUsuario respuesta(Simulacro simulacro) {
        RespuestaUsuario respuesta = new RespuestaUsuario(simulacro, pregunta(PRESENTACION), null, LocalDateTime.now());
        respuesta.setId(100L);
        return respuesta;
    }

    @Test
    void elProblemaSaleDelJsonGuardadoSinLaFiguraNula() {
        PresentacionResponse problema = PresentacionMapper.problema(pregunta(PRESENTACION));

        assertThat(problema.enunciado()).isEqualTo("Halle $x$ si $2x = 4$.");
        assertThat(problema.alternativas()).hasSize(5);
        assertThat(problema.figura()).isNull();
        assertThat(PresentacionMapper.pasos(pregunta(PRESENTACION)))
                .singleElement()
                .satisfies(paso -> assertThat(paso.detalle()).isEqualTo("$$x = \\dfrac{4}{2} = 2$$"));
    }

    @Test
    void sinPresentacionSeUsaElTextoPlano() {
        assertThat(PresentacionMapper.problema(pregunta(null))).isNull();
        assertThat(PresentacionMapper.pasos(pregunta(null))).isNull();
    }

    @Test
    void unJsonDanadoNoRompeLaPregunta() {
        assertThat(PresentacionMapper.problema(pregunta("{no es json"))).isNull();
        assertThat(PresentacionMapper.pasos(pregunta("{no es json"))).isNull();
    }

    @Test
    void elSimulacroEnCursoLlevaElProblemaConFormatoPeroNoLosPasos() {
        Simulacro enCurso = simulacro(EstadoSimulacro.EN_CURSO);

        SimulacroResponse rendir = SimulacroMapper.toResponse(enCurso, List.of(respuesta(enCurso)));
        RespuestaDetalleResponse detalle = SimulacroMapper.toDetalle(enCurso, List.of(respuesta(enCurso)))
                .preguntas().get(0);

        assertThat(rendir.preguntas().get(0).presentacion().enunciado()).isEqualTo("Halle $x$ si $2x = 4$.");
        assertThat(detalle.presentacion()).isNotNull();
        assertThat(detalle.pasos()).isNull();
        assertThat(detalle.explicacion()).isNull();
    }

    @Test
    void alFinalizarElSimulacroSeRevelanLosPasos() {
        Simulacro finalizado = simulacro(EstadoSimulacro.FINALIZADO);

        SimulacroDetalleResponse detalle = SimulacroMapper.toDetalle(finalizado, List.of(respuesta(finalizado)));

        assertThat(detalle.preguntas().get(0).pasos()).hasSize(1);
    }
}
