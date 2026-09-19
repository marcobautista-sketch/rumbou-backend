package com.rumbou.backend.repository;

import com.rumbou.backend.entity.Area;
import com.rumbou.backend.entity.Carrera;
import com.rumbou.backend.entity.OfertaAcademica;
import com.rumbou.backend.entity.Universidad;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OfertaAcademicaRepositoryTest extends AbstractContainerBaseTest {

    @Autowired
    private OfertaAcademicaRepository ofertaAcademicaRepository;

    @Autowired
    private EntityManager entityManager;

    private OfertaAcademica sistemasUni;
    private OfertaAcademica civilUni;
    private OfertaAcademica sistemasUnmsm;

    @BeforeEach
    void crearCatalogo() {
        Universidad uni = persistir(new Universidad("Universidad Nacional de Ingenieria", "UNI-TEST", 1800, 180));
        Universidad unmsm = persistir(new Universidad("Universidad Nacional Mayor de San Marcos", "UNMSM-TEST", 2000, 100));
        Area areaGeneralUni = persistir(new Area(uni, "GENERAL-TEST", "General"));
        Area areaCUnmsm = persistir(new Area(unmsm, "C-TEST", "Ingenierias"));

        Carrera sistemas = persistir(new Carrera("Ingenieria de Sistemas (test)", "Facultad de prueba"));
        Carrera civil = persistir(new Carrera("Ingenieria Civil (test)", "Facultad de prueba"));

        sistemasUni = persistir(new OfertaAcademica(uni, sistemas, areaGeneralUni, "2026-II", 1209, 31));
        civilUni = persistir(new OfertaAcademica(uni, civil, areaGeneralUni, "2026-II", 1132.2, 47));
        sistemasUnmsm = persistir(new OfertaAcademica(unmsm, sistemas, areaCUnmsm, "2026-II", 1268, 32));
        entityManager.flush();
    }

    @Test
    void sinFiltrosDevuelveTodasLasOfertas() {
        List<OfertaAcademica> ofertas = ofertaAcademicaRepository.buscar(null, null, null);

        assertThat(ofertas).extracting(OfertaAcademica::getId)
                .contains(sistemasUni.getId(), civilUni.getId(), sistemasUnmsm.getId());
    }

    @Test
    void filtraPorUniversidad() {
        List<OfertaAcademica> ofertas = ofertaAcademicaRepository.buscar("UNI-TEST", null, null);

        assertThat(ofertas).extracting(OfertaAcademica::getId)
                .containsExactlyInAnyOrder(sistemasUni.getId(), civilUni.getId());
    }

    @Test
    void filtraPorAreaDeLaUniversidad() {
        List<OfertaAcademica> ofertas = ofertaAcademicaRepository.buscar(null, "C-TEST", null);

        assertThat(ofertas).extracting(OfertaAcademica::getId)
                .containsExactly(sistemasUnmsm.getId());
    }

    // El postulante escribe "sistemas" en minuscula y encuentra "Ingenieria de Sistemas".
    @Test
    void buscaLaCarreraPorCoincidenciaParcialSinDistinguirMayusculas() {
        List<OfertaAcademica> ofertas = ofertaAcademicaRepository.buscar(null, null, "sistemas");

        assertThat(ofertas).extracting(OfertaAcademica::getId)
                .containsExactlyInAnyOrder(sistemasUni.getId(), sistemasUnmsm.getId());
    }

    @Test
    void combinaLosTresFiltros() {
        assertThat(ofertaAcademicaRepository.buscar("UNI-TEST", "GENERAL-TEST", "civil"))
                .extracting(OfertaAcademica::getId)
                .containsExactly(civilUni.getId());

        // La carrera existe, pero no en esa universidad: no hay resultados.
        assertThat(ofertaAcademicaRepository.buscar("UNMSM-TEST", null, "civil")).isEmpty();
    }

    private <T> T persistir(T entidad) {
        entityManager.persist(entidad);
        return entidad;
    }
}
