package com.rumbou.backend.repository;

import com.rumbou.backend.entity.Area;
import com.rumbou.backend.entity.AreaConocimiento;
import com.rumbou.backend.entity.Dificultad;
import com.rumbou.backend.entity.EstadoSimulacro;
import com.rumbou.backend.entity.OrigenPregunta;
import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.entity.RespuestaUsuario;
import com.rumbou.backend.entity.Role;
import com.rumbou.backend.entity.Simulacro;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.entity.TipoSimulacro;
import com.rumbou.backend.entity.Universidad;
import com.rumbou.backend.entity.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// Las consultas que usa progreso sobre SimulacroRepository y RespuestaUsuarioRepository.
class ConsultasProgresoRepositoryTest extends AbstractContainerBaseTest {

    @Autowired
    private SimulacroRepository simulacroRepository;

    @Autowired
    private RespuestaUsuarioRepository respuestaUsuarioRepository;

    @Autowired
    private EntityManager entityManager;

    private Usuario postulante;
    private Usuario otroPostulante;
    private Area areaGeneral;
    private Area otraArea;
    private Tema algebra;
    private Tema fisica;

    @BeforeEach
    void crearDatos() {
        postulante = persistir(new Usuario("postulante@rumbou.com", "hash", "Ana", Role.USER));
        otroPostulante = persistir(new Usuario("otro@rumbou.com", "hash", "Luis", Role.USER));
        Universidad uni = persistir(new Universidad("Universidad Nacional de Ingenieria", "UNI-TEST", 1800, 180));
        areaGeneral = persistir(new Area(uni, "GENERAL", "General"));
        otraArea = persistir(new Area(uni, "OTRA", "Otra area de prueba"));
        algebra = persistir(new Tema("Algebra (test)", AreaConocimiento.MATEMATICA));
        fisica = persistir(new Tema("Fisica (test)", AreaConocimiento.FISICA_QUIMICA));
    }

    @Test
    void elUltimoSimulacroFinalizadoIgnoraLosQueSiguenEnCurso() {
        finalizado(postulante, areaGeneral, 900, LocalDateTime.of(2026, 9, 10, 10, 0));
        finalizado(postulante, areaGeneral, 1100, LocalDateTime.of(2026, 9, 18, 10, 0));
        persistir(new Simulacro(postulante, areaGeneral, TipoSimulacro.COMPLETO, LocalDateTime.of(2026, 9, 19, 9, 0)));

        Simulacro ultimo = simulacroRepository
                .findFirstByUsuarioIdAndAreaIdAndEstadoOrderByFechaFinDesc(
                        postulante.getId(), areaGeneral.getId(), EstadoSimulacro.FINALIZADO)
                .orElseThrow();

        assertThat(ultimo.getPsp()).isEqualTo(1100);
    }

    @Test
    void elHistoricoSoloTraeSimulacrosFinalizadosDelUsuarioEnEsaAreaEnOrdenCronologico() {
        finalizado(postulante, areaGeneral, 1100, LocalDateTime.of(2026, 9, 18, 10, 0));
        finalizado(postulante, areaGeneral, 900, LocalDateTime.of(2026, 9, 10, 10, 0));
        finalizado(postulante, otraArea, 500, LocalDateTime.of(2026, 9, 12, 10, 0));
        finalizado(otroPostulante, areaGeneral, 1500, LocalDateTime.of(2026, 9, 11, 10, 0));
        persistir(new Simulacro(postulante, areaGeneral, TipoSimulacro.COMPLETO, LocalDateTime.of(2026, 9, 19, 9, 0)));

        List<Simulacro> historial = simulacroRepository.findByUsuarioIdAndAreaIdAndEstadoOrderByFechaFinAsc(
                postulante.getId(), areaGeneral.getId(), EstadoSimulacro.FINALIZADO);

        assertThat(historial).extracting(Simulacro::getPsp).containsExactly(900.0, 1100.0);
    }

    @Test
    void cuentaCorrectasIncorrectasYEnBlancoPorTemaSoloDeSimulacrosFinalizados() {
        Simulacro terminado = finalizado(postulante, areaGeneral, 1000, LocalDateTime.of(2026, 9, 18, 10, 0));
        responder(terminado, algebra, 0, true);
        responder(terminado, algebra, 0, true);
        responder(terminado, algebra, 1, false);
        responder(terminado, algebra, null, null);
        responder(terminado, fisica, 2, false);

        Simulacro enCurso = persistir(new Simulacro(postulante, areaGeneral, TipoSimulacro.COMPLETO, LocalDateTime.now()));
        responder(enCurso, algebra, 0, true);

        Simulacro deOtro = finalizado(otroPostulante, areaGeneral, 1000, LocalDateTime.of(2026, 9, 18, 11, 0));
        responder(deOtro, fisica, 0, true);

        Map<String, RespuestaUsuarioRepository.ConteoPorTema> porTema = respuestaUsuarioRepository
                .contarRespuestasPorTema(postulante.getId(), EstadoSimulacro.FINALIZADO).stream()
                .collect(Collectors.toMap(RespuestaUsuarioRepository.ConteoPorTema::getTema, Function.identity()));

        assertThat(porTema).containsOnlyKeys("Algebra (test)", "Fisica (test)");
        RespuestaUsuarioRepository.ConteoPorTema conteoAlgebra = porTema.get("Algebra (test)");
        assertThat(conteoAlgebra.getCorrectas()).isEqualTo(2);
        assertThat(conteoAlgebra.getIncorrectas()).isEqualTo(1);
        assertThat(conteoAlgebra.getEnBlanco()).isEqualTo(1);
        RespuestaUsuarioRepository.ConteoPorTema conteoFisica = porTema.get("Fisica (test)");
        assertThat(conteoFisica.getCorrectas()).isZero();
        assertThat(conteoFisica.getIncorrectas()).isEqualTo(1);
        assertThat(conteoFisica.getEnBlanco()).isZero();
    }

    private Simulacro finalizado(Usuario usuario, Area area, double psp, LocalDateTime fechaFin) {
        Simulacro simulacro = new Simulacro(usuario, area, TipoSimulacro.COMPLETO, fechaFin.minusHours(3));
        simulacro.setEstado(EstadoSimulacro.FINALIZADO);
        simulacro.setPsp(psp);
        simulacro.setPuntajeObtenido(psp / 2);
        simulacro.setFechaFin(fechaFin);
        return persistir(simulacro);
    }

    // Deja la respuesta como la deja CalificadorService: esCorrecta null si quedo en blanco.
    private void responder(Simulacro simulacro, Tema tema, Integer alternativaMarcada, Boolean esCorrecta) {
        Pregunta pregunta = persistir(new Pregunta(tema, "Pregunta de " + tema.getNombre(),
                List.of("A", "B", "C", "D", "E"), 0, "explicacion", Dificultad.MEDIA, OrigenPregunta.SEMILLA, true));
        RespuestaUsuario respuesta = new RespuestaUsuario(simulacro, pregunta, alternativaMarcada, LocalDateTime.now());
        respuesta.setEsCorrecta(esCorrecta);
        persistir(respuesta);
    }

    private <T> T persistir(T entidad) {
        entityManager.persist(entidad);
        return entidad;
    }
}
