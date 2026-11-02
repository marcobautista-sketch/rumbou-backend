package com.rumbou.backend.seed;

import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.repository.AbstractContainerBaseTest;
import com.rumbou.backend.repository.AreaRepository;
import com.rumbou.backend.repository.CarreraRepository;
import com.rumbou.backend.repository.EsquemaCalificacionRepository;
import com.rumbou.backend.repository.EstructuraExamenRepository;
import com.rumbou.backend.repository.OfertaAcademicaRepository;
import com.rumbou.backend.repository.PreguntaRepository;
import com.rumbou.backend.repository.TemaRepository;
import com.rumbou.backend.repository.UniversidadRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

// Contra PostgreSQL real: la segunda corrida actualiza preguntas que ya existen,
// que es el caso de volver a ejecutar el profile "seed" sobre una base cargada.
class ContenidoSeedRunnerTest extends AbstractContainerBaseTest {

    @Autowired
    private UniversidadRepository universidadRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private EsquemaCalificacionRepository esquemaCalificacionRepository;

    @Autowired
    private TemaRepository temaRepository;

    @Autowired
    private EstructuraExamenRepository estructuraExamenRepository;

    @Autowired
    private CarreraRepository carreraRepository;

    @Autowired
    private OfertaAcademicaRepository ofertaAcademicaRepository;

    @Autowired
    private PreguntaRepository preguntaRepository;

    @Autowired
    private EntityManager entityManager;

    private ContenidoSeedRunner seed;

    @BeforeEach
    void cargarCatalogo() {
        new AcademicoSeedRunner(universidadRepository, areaRepository, esquemaCalificacionRepository,
                temaRepository, estructuraExamenRepository, carreraRepository, ofertaAcademicaRepository).run();
        seed = new ContenidoSeedRunner(preguntaRepository, temaRepository);
    }

    @Test
    void volverACorrerElSeedActualizaLasPreguntasSinDuplicarlas() {
        seed.run();
        entityManager.flush();
        entityManager.clear();
        long cargadas = preguntaRepository.count();

        seed.run();
        entityManager.flush();
        entityManager.clear();

        assertThat(cargadas).isEqualTo(792);
        assertThat(preguntaRepository.count()).isEqualTo(cargadas);
    }

    @Test
    void laSegundaCorridaConservaLasCincoAlternativas() {
        seed.run();
        entityManager.flush();
        entityManager.clear();

        seed.run();
        entityManager.flush();
        entityManager.clear();

        Pregunta pregunta = preguntaRepository.findAll().get(0);
        assertThat(pregunta.getAlternativas()).hasSize(5);
    }

    @Test
    void unEnunciadoCorregidoSeAplicaALaPreguntaQueYaExistiaSinDuplicarla() {
        seed.run();
        entityManager.flush();
        entityManager.clear();
        ArchivoSeed.Fila correccion = ArchivoSeed.leer("enunciados-corregidos.csv", 3).get(0);
        Long temaId = temaRepository.findByNombre(correccion.texto(0)).orElseThrow().getId();
        // La base queda como en produccion antes de la correccion: con el enunciado anterior.
        Pregunta pregunta = preguntaRepository.findByTemaIdAndEnunciado(temaId, correccion.texto(2)).orElseThrow();
        pregunta.setEnunciado(correccion.texto(1));
        preguntaRepository.save(pregunta);
        entityManager.flush();
        entityManager.clear();

        seed.run();
        entityManager.flush();
        entityManager.clear();

        assertThat(preguntaRepository.count()).isEqualTo(792);
        Pregunta corregida = preguntaRepository.findById(pregunta.getId()).orElseThrow();
        assertThat(corregida.getEnunciado()).isEqualTo(correccion.texto(2));
        assertThat(corregida.getPresentacion()).isNotNull();
    }

    @Test
    void cadaFichaDePresentacionesQuedaGuardadaEnSuPregunta() {
        seed.run();
        entityManager.flush();
        entityManager.clear();

        long conFormato = preguntaRepository.findAll().stream()
                .filter(pregunta -> pregunta.getPresentacion() != null)
                .count();
        assertThat(conFormato).isEqualTo(PresentacionesSeed.leer().size());
    }
}
