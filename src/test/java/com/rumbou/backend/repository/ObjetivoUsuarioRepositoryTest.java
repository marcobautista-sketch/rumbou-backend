package com.rumbou.backend.repository;

import com.rumbou.backend.entity.Area;
import com.rumbou.backend.entity.Carrera;
import com.rumbou.backend.entity.ObjetivoUsuario;
import com.rumbou.backend.entity.OfertaAcademica;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Universidad;
import com.rumbou.backend.entity.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObjetivoUsuarioRepositoryTest extends AbstractContainerBaseTest {

    @Autowired
    private ObjetivoUsuarioRepository objetivoUsuarioRepository;

    @Autowired
    private EntityManager entityManager;

    private Usuario postulante;
    private Usuario otroPostulante;
    private Area areaGeneralUni;
    private OfertaAcademica sistemasUni;
    private OfertaAcademica civilUni;
    private OfertaAcademica sistemasUnmsm;

    @BeforeEach
    void crearCatalogo() {
        postulante = persistir(new Usuario("postulante@rumbou.com", "hash", "Ana", Role.USER));
        otroPostulante = persistir(new Usuario("otro@rumbou.com", "hash", "Luis", Role.USER));

        Universidad uni = persistir(new Universidad("Universidad Nacional de Ingenieria", "UNI-TEST", 1800, 180));
        Universidad unmsm = persistir(new Universidad("Universidad Nacional Mayor de San Marcos", "UNMSM-TEST", 2000, 100));
        areaGeneralUni = persistir(new Area(uni, "GENERAL", "General"));
        Area areaCUnmsm = persistir(new Area(unmsm, "C", "Ingenierias"));

        Carrera sistemas = persistir(new Carrera("Ingenieria de Sistemas (test)", "Facultad de prueba"));
        Carrera civil = persistir(new Carrera("Ingenieria Civil (test)", "Facultad de prueba"));

        sistemasUni = persistir(new OfertaAcademica(uni, sistemas, areaGeneralUni, "2026-II", 1209, 31));
        civilUni = persistir(new OfertaAcademica(uni, civil, areaGeneralUni, "2026-II", 1132.2, 47));
        sistemasUnmsm = persistir(new OfertaAcademica(unmsm, sistemas, areaCUnmsm, "2026-II", 1268, 32));
    }

    @Test
    void soloCuentaLosObjetivosActivosDelUsuario() {
        guardarObjetivo(postulante, sistemasUni, true);
        guardarObjetivo(postulante, civilUni, false);
        guardarObjetivo(otroPostulante, sistemasUnmsm, true);

        List<ObjetivoUsuario> activos = objetivoUsuarioRepository.findByUsuarioIdAndActivoTrue(postulante.getId());

        assertThat(activos).hasSize(1);
        assertThat(activos.get(0).getOfertaAcademica().getId()).isEqualTo(sistemasUni.getId());
        assertThat(objetivoUsuarioRepository.countByUsuarioIdAndActivoTrue(postulante.getId())).isEqualTo(1);
    }

    @Test
    void buscaLosObjetivosActivosDeUnAreaParaActualizarlosTrasUnSimulacro() {
        guardarObjetivo(postulante, sistemasUni, true);
        guardarObjetivo(postulante, civilUni, false);
        guardarObjetivo(postulante, sistemasUnmsm, true);

        List<ObjetivoUsuario> delArea = objetivoUsuarioRepository
                .findByUsuarioIdAndActivoTrueAndOfertaAcademicaAreaId(postulante.getId(), areaGeneralUni.getId());

        assertThat(delArea).hasSize(1);
        assertThat(delArea.get(0).getOfertaAcademica().getId()).isEqualTo(sistemasUni.getId());
    }

    @Test
    void encuentraElObjetivoPorOfertaAunqueEsteDesactivado() {
        guardarObjetivo(postulante, civilUni, false);

        assertThat(objetivoUsuarioRepository
                .findByUsuarioIdAndOfertaAcademicaId(postulante.getId(), civilUni.getId()))
                .isPresent()
                .get()
                .extracting(ObjetivoUsuario::isActivo)
                .isEqualTo(false);
    }

    @Test
    void laBaseNoPermiteRepetirLaMismaOfertaParaElMismoUsuario() {
        guardarObjetivo(postulante, sistemasUni, true);

        ObjetivoUsuario repetido = new ObjetivoUsuario(postulante, sistemasUni, LocalDateTime.now());

        assertThatThrownBy(() -> objetivoUsuarioRepository.saveAndFlush(repetido))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void guardarObjetivo(Usuario usuario, OfertaAcademica oferta, boolean activo) {
        ObjetivoUsuario objetivo = new ObjetivoUsuario(usuario, oferta, LocalDateTime.now());
        objetivo.setActivo(activo);
        objetivoUsuarioRepository.saveAndFlush(objetivo);
    }

    private <T> T persistir(T entidad) {
        entityManager.persist(entidad);
        return entidad;
    }
}
