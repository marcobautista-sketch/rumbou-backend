package com.rumbou.backend.listener;

import com.rumbou.backend.entity.Area;
import com.rumbou.backend.entity.Carrera;
import com.rumbou.backend.entity.ObjetivoUsuario;
import com.rumbou.backend.entity.OfertaAcademica;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Universidad;
import com.rumbou.backend.entity.Usuario;
import com.rumbou.backend.event.SimulacroFinalizadoEvent;
import com.rumbou.backend.repository.AbstractContainerBaseTest;
import com.rumbou.backend.service.PlanService;
import com.rumbou.backend.service.ProgresoService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;

// Publica el evento real con el publicador de Spring y revisa la base: prueba que el
// listener esta registrado, que es sincrono y que corre en la misma transaccion.
@Import({ProgresoListener.class, ProgresoService.class, PlanService.class})
class ProgresoListenerTest extends AbstractContainerBaseTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private EntityManager entityManager;

    private Usuario postulante;
    private Area areaGeneralUni;
    private OfertaAcademica sistemasUni;
    private OfertaAcademica sistemasUnmsm;

    @BeforeEach
    void crearCatalogo() {
        postulante = persistir(new Usuario("postulante@rumbou.com", "hash", "Ana", Role.USER));

        Universidad uni = persistir(new Universidad("Universidad Nacional de Ingenieria", "UNI-TEST", 1800, 180));
        Universidad unmsm = persistir(new Universidad("Universidad Nacional Mayor de San Marcos", "UNMSM-TEST", 2000, 100));
        areaGeneralUni = persistir(new Area(uni, "GENERAL", "General"));
        Area areaCUnmsm = persistir(new Area(unmsm, "C", "Ingenierias"));
        Carrera sistemas = persistir(new Carrera("Ingenieria de Sistemas (test)", "Facultad de prueba"));

        sistemasUni = persistir(new OfertaAcademica(uni, sistemas, areaGeneralUni, "2026-II", 1209, 31));
        sistemasUnmsm = persistir(new OfertaAcademica(unmsm, sistemas, areaCUnmsm, "2026-II", 1268, 32));
    }

    @Test
    void alFinalizarUnSimulacroActualizaElPspYElIpDelObjetivoDeEsaArea() {
        ObjetivoUsuario objetivo = persistir(new ObjetivoUsuario(postulante, sistemasUni, LocalDateTime.now()));

        eventPublisher.publishEvent(new SimulacroFinalizadoEvent(99L, postulante.getId(),
                areaGeneralUni.getId(), 1000, 1330));

        ObjetivoUsuario actualizado = recargar(objetivo);
        assertThat(actualizado.getUltimoPsp()).isEqualTo(1330);
        assertThat(actualizado.getUltimoIp()).isCloseTo(1.1, within(0.0001));
        assertThat(actualizado.getFechaActualizacion()).isNotNull();
    }

    @Test
    void unSimulacroDeOtraAreaNoTocaElObjetivo() {
        ObjetivoUsuario deUnmsm = persistir(new ObjetivoUsuario(postulante, sistemasUnmsm, LocalDateTime.now()));

        eventPublisher.publishEvent(new SimulacroFinalizadoEvent(99L, postulante.getId(),
                areaGeneralUni.getId(), 1000, 1330));

        assertThat(recargar(deUnmsm).getUltimoPsp()).isNull();
    }

    @Test
    void unUsuarioSinObjetivosNoRompeLaFinalizacionDelSimulacro() {
        assertThatCode(() -> eventPublisher.publishEvent(new SimulacroFinalizadoEvent(99L, postulante.getId(),
                areaGeneralUni.getId(), 1000, 1330)))
                .doesNotThrowAnyException();
    }

    private ObjetivoUsuario recargar(ObjetivoUsuario objetivo) {
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(ObjetivoUsuario.class, objetivo.getId());
    }

    private <T> T persistir(T entidad) {
        entityManager.persist(entidad);
        return entidad;
    }
}
