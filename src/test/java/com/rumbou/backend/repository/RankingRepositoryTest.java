package com.rumbou.backend.repository;

import com.rumbou.backend.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class RankingRepositoryTest extends AbstractContainerBaseTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);

    @Autowired
    private EntityManager entidades;
    @Autowired
    private RankingRepository ranking;
    @Autowired
    private ObjetivoUsuarioRepository objetivos;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private SimulacroRepository simulacros;

    private OfertaAcademica uni;
    private OfertaAcademica unmsm;
    private int secuencia;

    @BeforeEach
    void catalogo() {
        uni = oferta("UNI");
        unmsm = oferta("UNMSM");
    }

    @Test
    void incluyeSoloPostulantesInscritosVigentesConMetaPrincipalEnLaUniversidad() {
        Usuario incluido = usuario(Role.USER, "Lucero", 120, LUNES);
        objetivo(incluido, uni, true);
        // Varias metas en la misma universidad tampoco deben duplicar al participante.
        OfertaAcademica secundariaUni = persistir(new OfertaAcademica(uni.getUniversidad(), uni.getCarrera(),
                uni.getArea(), "2027-I", 1000, 30));
        objetivo(incluido, secundariaUni, true);
        List<Usuario> excluidos = new ArrayList<>();
        excluidos.add(usuario(Role.ADMIN, "Admin", 900, LUNES));
        excluidos.add(usuario(Role.REVIEWER, "Revisor", 900, LUNES));
        excluidos.add(usuario(Role.USER, null, 900, LUNES));
        excluidos.add(usuario(Role.USER, "Vencido", 900, LUNES.minusDays(1)));
        excluidos.add(usuario(Role.USER, "SinFecha", 900, null));
        excluidos.add(usuario(Role.USER, "SinXp", 0, LUNES));
        excluidos.forEach(usuario -> objetivo(usuario, uni, true));
        Usuario otraUniversidad = usuario(Role.USER, "OtraUniversidad", 900, LUNES);
        objetivo(otraUniversidad, unmsm, true);
        excluidos.add(otraUniversidad);
        Usuario soloSecundaria = usuario(Role.USER, "MetaSecundaria", 900, LUNES);
        objetivo(soloSecundaria, unmsm, true);
        objetivo(soloSecundaria, uni, true);
        excluidos.add(soloSecundaria);
        Usuario inactivo = usuario(Role.USER, "MetaInactiva", 900, LUNES);
        objetivo(inactivo, uni, false);
        excluidos.add(inactivo);
        excluidos.add(usuario(Role.USER, "SinMeta", 900, LUNES));
        entidades.flush();
        entidades.clear();

        var pagina = ranking.obtenerParticipantes(uni.getUniversidad().getId(), LUNES, PageRequest.of(0, 1));

        assertThat(pagina.getContent()).extracting(RankingRepository.Participante::getId)
                .containsExactly(incluido.getId());
        assertThat(pagina.getContent().getFirst().getAlias()).isEqualTo("Lucero");
        assertThat(pagina.getContent().getFirst().getXpSemanal()).isEqualTo(120);
        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(ranking.obtenerPosicion(uni.getUniversidad().getId(), LUNES, incluido.getId())).isEqualTo(1L);
        for (Usuario excluido : excluidos) {
            assertThat(ranking.obtenerPosicion(uni.getUniversidad().getId(), LUNES, excluido.getId()))
                    .as("posicion del excluido %s", excluido.getAliasLiga()).isNull();
        }
    }

    @Test
    void losEmpatesSonEstablesEntrePaginasYLaPosicionCoincide() {
        Usuario primeroEnEmpate = usuario(Role.USER, "Primero", 200, LUNES);
        Usuario segundoEnEmpate = usuario(Role.USER, "Segundo", 200, LUNES.plusDays(1));
        Usuario lider = usuario(Role.USER, "Lider", 300, LUNES);
        Usuario ultimo = usuario(Role.USER, "Ultimo", 100, LUNES);
        List<Usuario> orden = List.of(lider, primeroEnEmpate, segundoEnEmpate, ultimo);
        orden.forEach(usuario -> objetivo(usuario, uni, true));
        entidades.flush();
        entidades.clear();

        List<Long> ids = new ArrayList<>();
        for (int numero = 0; numero < 2; numero++) {
            var pagina = ranking.obtenerParticipantes(uni.getUniversidad().getId(), LUNES, PageRequest.of(numero, 2));
            ids.addAll(pagina.getContent().stream().map(RankingRepository.Participante::getId).toList());
            assertThat(pagina.getTotalElements()).isEqualTo(4);
            assertThat(pagina.getTotalPages()).isEqualTo(2);
        }
        assertThat(ids).containsExactlyElementsOf(orden.stream().map(Usuario::getId).toList());
        for (int indice = 0; indice < ids.size(); indice++) {
            assertThat(ranking.obtenerPosicion(uni.getUniversidad().getId(), LUNES, ids.get(indice)))
                    .isEqualTo(indice + 1L);
        }
        assertThat(ranking.obtenerParticipantes(uni.getUniversidad().getId(), LUNES, PageRequest.of(2, 2)))
                .isEmpty();
    }

    @Test
    void laMetaPrincipalEsLaActivaConMenorIdYNoLaPrimeraCreadaPorFecha() {
        Usuario usuario = usuario(Role.USER, "Lucero", 100, LUNES);
        objetivo(usuario, unmsm, false);
        ObjetivoUsuario principal = objetivo(usuario, uni, true);
        OfertaAcademica otra = persistir(new OfertaAcademica(unmsm.getUniversidad(), unmsm.getCarrera(),
                unmsm.getArea(), "2027-I", 1000, 30));
        objetivo(usuario, otra, true).setFechaCreacion(LocalDateTime.of(2020, 1, 1, 0, 0));
        entidades.flush();

        assertThat(objetivos.findFirstByUsuarioIdAndActivoTrueOrderByIdAsc(usuario.getId()))
                .get().extracting(ObjetivoUsuario::getId).isEqualTo(principal.getId());
        assertThat(ranking.obtenerPosicion(uni.getUniversidad().getId(), LUNES, usuario.getId())).isEqualTo(1L);
        assertThat(ranking.obtenerPosicion(unmsm.getUniversidad().getId(), LUNES, usuario.getId())).isNull();
    }

    @Test
    void elAliasNoDistingueMayusculasNiAcentosEnMayusculasPeroPermiteElPropio() {
        Usuario titular = usuario(Role.USER, "Ñandú", 100, LUNES);
        Usuario otro = usuario(Role.USER, null, 100, LUNES);
        entidades.flush();

        assertThat(usuarios.reservarAliasLiga("ñANDÚ")).isEqualTo(1);
        assertThat(usuarios.existeAliasLigaDeOtroUsuario("ñANDÚ", otro.getId())).isTrue();
        assertThat(usuarios.existeAliasLigaDeOtroUsuario("ñANDÚ", titular.getId())).isFalse();
    }

    @Test
    void laRestriccionUnicaRechazaUnDuplicadoExacto() {
        usuario(Role.USER, "Lucero", 100, LUNES);
        entidades.flush();
        Usuario duplicado = new Usuario("duplicado@example.com", "hash", "Privado", Role.USER);
        duplicado.setAliasLiga("Lucero");

        assertThatThrownBy(() -> usuarios.saveAndFlush(duplicado)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void elCierreCondicionalDelSimulacroSoloSeObtieneUnaVez() {
        Usuario usuario = usuario(Role.USER, null, 0, null);
        Simulacro simulacro = persistir(new Simulacro(usuario, uni.getArea(), TipoSimulacro.COMPLETO,
                LocalDateTime.of(2026, 11, 2, 10, 0)));
        entidades.flush();

        assertThat(simulacros.marcarFinalizadoSiEnCurso(simulacro.getId())).isEqualTo(1);
        assertThat(simulacros.marcarFinalizadoSiEnCurso(simulacro.getId())).isZero();
        entidades.clear();
        assertThat(simulacros.findById(simulacro.getId())).get()
                .extracting(Simulacro::getEstado).isEqualTo(EstadoSimulacro.FINALIZADO);
    }

    private OfertaAcademica oferta(String siglas) {
        Universidad universidad = persistir(new Universidad(siglas, siglas, 1800, 180));
        Area area = persistir(new Area(universidad, "GENERAL", "General"));
        Carrera carrera = persistir(new Carrera("Carrera " + siglas, "Facultad"));
        return persistir(new OfertaAcademica(universidad, carrera, area, "2026-II", 1000, 30));
    }

    private Usuario usuario(Role rol, String alias, int xp, LocalDate fecha) {
        Usuario usuario = new Usuario("ranking" + (++secuencia) + "@example.com", "hash", "Nombre privado", rol);
        usuario.setAliasLiga(alias);
        usuario.setXpSemanal(xp);
        usuario.setLastActivityDate(fecha);
        return persistir(usuario);
    }

    private ObjetivoUsuario objetivo(Usuario usuario, OfertaAcademica oferta, boolean activo) {
        ObjetivoUsuario objetivo = new ObjetivoUsuario(usuario, oferta, LocalDateTime.of(2026, 11, 2, 10, 0));
        objetivo.setActivo(activo);
        return persistir(objetivo);
    }

    private <T> T persistir(T entidad) {
        entidades.persist(entidad);
        return entidad;
    }
}
