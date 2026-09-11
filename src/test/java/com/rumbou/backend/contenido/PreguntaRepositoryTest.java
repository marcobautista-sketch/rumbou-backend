package com.rumbou.backend.contenido;

import com.rumbou.backend.academico.AreaConocimiento;
import com.rumbou.backend.academico.Tema;
import com.rumbou.backend.shared.AbstractContainerBaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PreguntaRepositoryTest extends AbstractContainerBaseTest {

    @Autowired
    private PreguntaRepository preguntaRepository;

    private Tema temaAlgebra;
    private Tema temaGeometria;

    // No hay TemaRepository todavia (le toca a academico/, ver reparto de trabajo),
    // asi que persistimos el Tema directamente con el EntityManager de prueba.
    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    void crearTemas() {
        temaAlgebra = new Tema("Algebra", AreaConocimiento.MATEMATICA);
        temaGeometria = new Tema("Geometria", AreaConocimiento.MATEMATICA);
        entityManager.persist(temaAlgebra);
        entityManager.persist(temaGeometria);

        guardar(temaAlgebra, "Pregunta facil aprobada de algebra", Dificultad.FACIL, OrigenPregunta.SEMILLA, true);
        guardar(temaAlgebra, "Pregunta dificil sin aprobar de algebra", Dificultad.DIFICIL, OrigenPregunta.IA_APROBADA, false);
        guardar(temaGeometria, "Pregunta media aprobada de geometria", Dificultad.MEDIA, OrigenPregunta.SEMILLA, true);
    }

    private void guardar(Tema tema, String enunciado, Dificultad dificultad, OrigenPregunta origen, boolean aprobada) {
        Pregunta pregunta = new Pregunta(tema, enunciado,
                List.of("A", "B", "C", "D", "E"), 0, "explicacion", dificultad, origen, aprobada);
        preguntaRepository.save(pregunta);
    }

    @Test
    void filtraPorTema() {
        Page<Pregunta> resultado = preguntaRepository.buscar(
                temaAlgebra.getId(), null, null, null, PageRequest.of(0, 10));

        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getContent())
                .allMatch(p -> p.getTema().getId().equals(temaAlgebra.getId()));
    }

    @Test
    void filtraPorDificultadYAprobada() {
        Page<Pregunta> resultado = preguntaRepository.buscar(
                null, Dificultad.FACIL, null, true, PageRequest.of(0, 10));

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEnunciado())
                .isEqualTo("Pregunta facil aprobada de algebra");
    }

    @Test
    void filtraPorOrigen() {
        Page<Pregunta> resultado = preguntaRepository.buscar(
                null, null, OrigenPregunta.IA_APROBADA, null, PageRequest.of(0, 10));

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getOrigen()).isEqualTo(OrigenPregunta.IA_APROBADA);
    }

    @Test
    void sinFiltrosDevuelveTodasPaginadas() {
        Page<Pregunta> resultado = preguntaRepository.buscar(
                null, null, null, null, PageRequest.of(0, 2));

        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertThat(resultado.getContent()).hasSize(2);
    }

    @Test
    void findByTemaIdAndAprobadaTrueSigueFuncionandoParaElModuloExamen() {
        List<Pregunta> resultado = preguntaRepository.findByTemaIdAndAprobadaTrue(temaAlgebra.getId());

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).isAprobada()).isTrue();
    }
}
