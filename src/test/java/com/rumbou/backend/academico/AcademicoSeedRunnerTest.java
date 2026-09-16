package com.rumbou.backend.academico;

import com.rumbou.backend.shared.AbstractContainerBaseTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

// Corre el seed contra un PostgreSQL real (TestContainers). El runner se construye
// a mano en vez de activar el profile "seed": asi corre dentro de la transaccion
// del test, que se deshace al terminar, y no deja datos en la base que comparten
// los demas tests de repositorio.
class AcademicoSeedRunnerTest extends AbstractContainerBaseTest {

    private static final String PROCESO = "2026-II";

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
    private EntityManager entityManager;

    private AcademicoSeedRunner seed;

    @BeforeEach
    void crearSeed() {
        seed = new AcademicoSeedRunner(universidadRepository, areaRepository,
                esquemaCalificacionRepository, temaRepository, estructuraExamenRepository,
                carreraRepository, ofertaAcademicaRepository);
    }

    @Test
    void cargaElCatalogoConLosValoresOficiales() {
        seed.run();

        Universidad uni = universidadRepository.findBySiglas("UNI").orElseThrow();
        Universidad unmsm = universidadRepository.findBySiglas("UNMSM").orElseThrow();
        assertThat(uni.getPuntajeMaximo()).isEqualTo(1800);
        assertThat(unmsm.getPuntajeMaximo()).isEqualTo(2000);

        EsquemaCalificacion conocimientos = esquemaCalificacionRepository
                .findByUniversidadIdAndNombreBloque(unmsm.getId(), "Conocimientos")
                .orElseThrow();
        assertThat(conocimientos.getValorAcierto()).isCloseTo(20.0, within(0.001));
        assertThat(conocimientos.getValorPenalidad()).isCloseTo(1.125, within(0.001));
        assertThat(conocimientos.getPuntajeMaximoBloque()).isCloseTo(1400.0, within(0.001));

        Tema biologia = temaRepository.findByNombre("Biología").orElseThrow();
        assertThat(biologia.getAreaConocimiento()).isEqualTo(AreaConocimiento.BIOLOGIA);
        assertThat(biologia.getTemario()).isNotBlank();
        assertThat(temaRepository.findByNombre("Actitud").orElseThrow().getTemario()).isNull();

        assertThat(preguntasDelArea(uni, "GENERAL")).isEqualTo(180);
        assertThat(preguntasDelArea(unmsm, "B")).isEqualTo(100);
        assertThat(preguntasDelArea(unmsm, "C")).isEqualTo(100);
    }

    @Test
    void cargaLasCarrerasYSusOfertasDeAdmision() {
        seed.run();
        entityManager.flush();

        assertThat(carreraRepository.count()).isEqualTo(51);
        assertThat(ofertaAcademicaRepository.count()).isEqualTo(70);
    }

    // El caso que justifica que OfertaAcademica sea un M:N con atributos: la misma
    // carrera existe en las dos universidades, con puntajes de corte distintos.
    @Test
    void laMismaCarreraTienePuntajeDistintoEnCadaUniversidad() {
        seed.run();

        Carrera sistemas = carreraRepository.findByNombre("Ingeniería de Sistemas").orElseThrow();
        Universidad uni = universidadRepository.findBySiglas("UNI").orElseThrow();
        Universidad unmsm = universidadRepository.findBySiglas("UNMSM").orElseThrow();

        assertThat(puntajeDeCorte(uni, sistemas, "GENERAL")).isCloseTo(1209.0, within(0.001));
        assertThat(puntajeDeCorte(unmsm, sistemas, "C")).isCloseTo(1268.0, within(0.001));
    }

    @Test
    void correrloDosVecesNoDuplicaNada() {
        seed.run();
        entityManager.flush();
        long[] despuesDeLaPrimeraCorrida = conteos();

        seed.run();
        entityManager.flush();

        assertThat(conteos()).containsExactly(despuesDeLaPrimeraCorrida);
    }

    @Test
    void volverACorrerloRestauraLosValoresDelArchivo() {
        seed.run();
        Universidad uni = universidadRepository.findBySiglas("UNI").orElseThrow();
        EsquemaCalificacion matematica = esquemaCalificacionRepository
                .findByUniversidadIdAndNombreBloque(uni.getId(), "Matemática")
                .orElseThrow();
        matematica.setValorPenalidad(99);
        esquemaCalificacionRepository.saveAndFlush(matematica);

        seed.run();
        entityManager.flush();
        entityManager.clear();

        EsquemaCalificacion restaurado = esquemaCalificacionRepository
                .findByUniversidadIdAndNombreBloque(uni.getId(), "Matemática")
                .orElseThrow();
        assertThat(restaurado.getValorPenalidad()).isCloseTo(3.0, within(0.001));
    }

    private int preguntasDelArea(Universidad universidad, String codigo) {
        Area area = areaRepository.findByUniversidadIdAndCodigo(universidad.getId(), codigo).orElseThrow();
        return estructuraExamenRepository.findByAreaIdOrderByOrden(area.getId()).stream()
                .mapToInt(EstructuraExamen::getCantidadPreguntas)
                .sum();
    }

    private double puntajeDeCorte(Universidad universidad, Carrera carrera, String codigoArea) {
        Area area = areaRepository.findByUniversidadIdAndCodigo(universidad.getId(), codigoArea).orElseThrow();
        return ofertaAcademicaRepository
                .findByUniversidadIdAndCarreraIdAndAreaIdAndProcesoAdmision(
                        universidad.getId(), carrera.getId(), area.getId(), PROCESO)
                .orElseThrow()
                .getPuntajeUltimoIngresante();
    }

    private long[] conteos() {
        return new long[] {
                universidadRepository.count(),
                areaRepository.count(),
                esquemaCalificacionRepository.count(),
                temaRepository.count(),
                estructuraExamenRepository.count(),
                carreraRepository.count(),
                ofertaAcademicaRepository.count()
        };
    }
}
