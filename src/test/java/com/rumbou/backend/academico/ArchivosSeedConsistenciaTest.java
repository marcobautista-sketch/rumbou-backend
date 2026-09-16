package com.rumbou.backend.academico;

import com.rumbou.backend.academico.ArchivoSeed.Fila;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

// JUnit puro, sin Spring ni base de datos: valida que los archivos del seed sean
// consistentes entre si y con los totales oficiales de cada examen. Si alguien
// edita un archivo y rompe la matematica del examen, falla este test y no un
// simulacro. Lee con ArchivoSeed, las mismas reglas de lectura que el seed real.
class ArchivosSeedConsistenciaTest {

    private static List<Fila> universidades;
    private static List<Fila> areas;
    private static List<Fila> esquemas;
    private static List<Fila> temas;
    private static List<Fila> estructura;
    private static List<Fila> carreras;
    private static List<Fila> ofertas;

    @BeforeAll
    static void leerArchivos() {
        universidades = ArchivoSeed.leer("universidades.csv", 4);
        areas = ArchivoSeed.leer("areas.csv", 3);
        esquemas = ArchivoSeed.leer("esquemas.csv", 6);
        temas = ArchivoSeed.leer("temas.csv", 3);
        estructura = ArchivoSeed.leer("estructura-examen.csv", 6);
        carreras = ArchivoSeed.leer("carreras.csv", 2);
        ofertas = ArchivoSeed.leer("ofertas-2026-II.csv", 6);
    }

    @Test
    void losBloquesDeCadaUniversidadSumanSuPuntajeMaximo() {
        for (Fila universidad : universidades) {
            String siglas = universidad.texto(0);

            double sumaDeBloques = esquemas.stream()
                    .filter(esquema -> esquema.texto(0).equals(siglas))
                    .mapToDouble(esquema -> esquema.decimal(4))
                    .sum();

            assertThat(sumaDeBloques)
                    .as("suma de puntaje_maximo_bloque de %s", siglas)
                    .isCloseTo(universidad.entero(2), within(0.001));
        }
    }

    @Test
    void losTemasTienenNombreUnicoYAreaDeConocimientoValida() {
        Set<String> areasDeConocimiento = Arrays.stream(AreaConocimiento.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(temas.stream().map(tema -> tema.texto(0)).toList()).doesNotHaveDuplicates();
        for (Fila tema : temas) {
            assertThat(areasDeConocimiento)
                    .as("area_conocimiento de %s", ubicacion(tema))
                    .contains(tema.texto(1));
        }
    }

    @Test
    void lasAreasYLosBloquesPertenecenAUniversidadesExistentes() {
        Set<String> siglas = universidades.stream().map(u -> u.texto(0)).collect(Collectors.toSet());

        for (Fila area : areas) {
            assertThat(siglas).as("universidad de %s", ubicacion(area)).contains(area.texto(0));
        }
        for (Fila esquema : esquemas) {
            assertThat(siglas).as("universidad de %s", ubicacion(esquema)).contains(esquema.texto(0));
        }
    }

    @Test
    void cadaFilaDeLaEstructuraReferenciaDatosExistentesDeSuUniversidad() {
        Set<String> areasExistentes = areas.stream()
                .map(area -> clave(area.texto(0), area.texto(1)))
                .collect(Collectors.toSet());
        Set<String> bloquesExistentes = esquemas.stream()
                .map(esquema -> clave(esquema.texto(0), esquema.texto(1)))
                .collect(Collectors.toSet());
        Set<String> temasExistentes = temas.stream()
                .map(tema -> tema.texto(0))
                .collect(Collectors.toSet());

        for (Fila fila : estructura) {
            assertThat(areasExistentes).as("area de %s", ubicacion(fila))
                    .contains(clave(fila.texto(0), fila.texto(1)));
            assertThat(bloquesExistentes).as("bloque de %s", ubicacion(fila))
                    .contains(clave(fila.texto(0), fila.texto(2)));
            assertThat(temasExistentes).as("tema de %s", ubicacion(fila))
                    .contains(fila.texto(3));
        }
    }

    // Hallazgo B de la auditoria: el esquema de puntaje se resuelve por tema al
    // finalizar un simulacro, asi que un tema no puede estar en dos bloques de la misma area.
    @Test
    void ningunTemaSeRepiteDentroDeUnaArea() {
        List<String> temasPorArea = estructura.stream()
                .map(fila -> clave(fila.texto(0), fila.texto(1), fila.texto(3)))
                .toList();

        assertThat(temasPorArea).doesNotHaveDuplicates();
    }

    @Test
    void cadaAreaSumaLasPreguntasYElPuntajeOficialDeSuUniversidad() {
        Map<String, Double> aciertoPorBloque = esquemas.stream()
                .collect(Collectors.toMap(e -> clave(e.texto(0), e.texto(1)), e -> e.decimal(2)));

        for (Fila area : areas) {
            String siglas = area.texto(0);
            String codigo = area.texto(1);
            Fila universidad = buscarUniversidad(siglas);
            List<Fila> filasDelArea = estructura.stream()
                    .filter(fila -> fila.texto(0).equals(siglas) && fila.texto(1).equals(codigo))
                    .toList();

            int preguntas = filasDelArea.stream().mapToInt(fila -> fila.entero(4)).sum();
            double puntos = filasDelArea.stream()
                    .mapToDouble(fila -> fila.entero(4) * aciertoPorBloque.get(clave(siglas, fila.texto(2))))
                    .sum();

            assertThat(filasDelArea).as("estructura del area %s %s", siglas, codigo).isNotEmpty();
            assertThat(preguntas).as("preguntas del area %s %s", siglas, codigo)
                    .isEqualTo(universidad.entero(3));
            assertThat(puntos).as("puntaje maximo del area %s %s", siglas, codigo)
                    .isCloseTo(universidad.entero(2), within(0.001));
        }
    }

    @Test
    void cadaBloqueDeCadaAreaSumaSuPuntajeMaximo() {
        for (Fila area : areas) {
            String siglas = area.texto(0);
            String codigo = area.texto(1);

            for (Fila esquema : esquemas) {
                if (!esquema.texto(0).equals(siglas)) {
                    continue;
                }
                String bloque = esquema.texto(1);

                double puntos = estructura.stream()
                        .filter(fila -> fila.texto(0).equals(siglas)
                                && fila.texto(1).equals(codigo)
                                && fila.texto(2).equals(bloque))
                        .mapToDouble(fila -> fila.entero(4) * esquema.decimal(2))
                        .sum();

                assertThat(puntos)
                        .as("bloque %s en el area %s %s", bloque, siglas, codigo)
                        .isCloseTo(esquema.decimal(4), within(0.001));
            }
        }
    }

    @Test
    void lasCarrerasTienenNombreUnico() {
        assertThat(carreras.stream().map(carrera -> carrera.texto(0)).toList()).doesNotHaveDuplicates();
    }

    @Test
    void cadaOfertaReferenciaUnAreaYUnaCarreraExistentes() {
        Set<String> areasExistentes = areas.stream()
                .map(area -> clave(area.texto(0), area.texto(1)))
                .collect(Collectors.toSet());
        Set<String> carrerasExistentes = carreras.stream()
                .map(carrera -> carrera.texto(0))
                .collect(Collectors.toSet());

        for (Fila oferta : ofertas) {
            assertThat(areasExistentes).as("area de %s", ubicacion(oferta))
                    .contains(clave(oferta.texto(0), oferta.texto(1)));
            assertThat(carrerasExistentes).as("carrera de %s", ubicacion(oferta))
                    .contains(oferta.texto(2));
        }
    }

    // La clave de OfertaAcademica es universidad + carrera + area + proceso:
    // la misma carrera puede repetirse en la otra universidad o en otro proceso.
    @Test
    void ningunaOfertaSeRepite() {
        List<String> claves = ofertas.stream()
                .map(oferta -> clave(oferta.texto(0), oferta.texto(1), oferta.texto(2), oferta.texto(3)))
                .toList();

        assertThat(claves).doesNotHaveDuplicates();
    }

    // Atrapa un error de escala facil de cometer: cargar el puntaje de UNI en la
    // escala vigesimal (13.43) en vez de la escala de 1800 puntos (1209).
    @Test
    void cadaPuntajeDeCorteEstaDentroDeLaEscalaDeSuUniversidad() {
        for (Fila oferta : ofertas) {
            String siglas = oferta.texto(0);
            double puntaje = oferta.decimal(4);
            int puntajeMaximo = buscarUniversidad(siglas).entero(2);

            assertThat(puntaje).as("puntaje del ultimo ingresante en %s", ubicacion(oferta))
                    .isGreaterThan(0)
                    .isLessThanOrEqualTo(puntajeMaximo);
        }
    }

    @Test
    void todaCarreraTieneAlMenosUnaOferta() {
        Set<String> carrerasConOferta = ofertas.stream()
                .map(oferta -> oferta.texto(2))
                .collect(Collectors.toSet());

        for (Fila carrera : carreras) {
            assertThat(carrerasConOferta).as("carrera sin oferta en %s", ubicacion(carrera))
                    .contains(carrera.texto(0));
        }
    }

    private static Fila buscarUniversidad(String siglas) {
        return universidades.stream()
                .filter(universidad -> universidad.texto(0).equals(siglas))
                .findFirst()
                .orElseThrow();
    }

    private static String clave(String... partes) {
        return String.join("|", partes);
    }

    private static String ubicacion(Fila fila) {
        return fila.archivo() + ", linea " + fila.numeroLinea();
    }
}
