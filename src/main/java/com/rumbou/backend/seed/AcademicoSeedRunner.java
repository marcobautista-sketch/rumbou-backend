package com.rumbou.backend.seed;

import com.rumbou.backend.entity.Area;
import com.rumbou.backend.entity.AreaConocimiento;
import com.rumbou.backend.entity.Carrera;
import com.rumbou.backend.entity.EsquemaCalificacion;
import com.rumbou.backend.entity.EstructuraExamen;
import com.rumbou.backend.entity.OfertaAcademica;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.entity.Universidad;
import com.rumbou.backend.repository.AreaRepository;
import com.rumbou.backend.repository.CarreraRepository;
import com.rumbou.backend.repository.EsquemaCalificacionRepository;
import com.rumbou.backend.repository.EstructuraExamenRepository;
import com.rumbou.backend.repository.OfertaAcademicaRepository;
import com.rumbou.backend.repository.TemaRepository;
import com.rumbou.backend.repository.UniversidadRepository;
import com.rumbou.backend.seed.ArchivoSeed.Fila;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Carga el catalogo academico desde los archivos de src/main/resources/seed/.
// Solo corre con el profile "seed" (decision del equipo: un runner por paquete,
// cada uno dueno de sembrar solo sus propias tablas, y nada de data.sql):
//   ./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
//
// Ningun valor del examen esta escrito aqui: este runner solo lee archivos (con
// ArchivoSeed) y los guarda. Corregir un puntaje o agregar un tema es editar un
// archivo, no el codigo.
//
// Es idempotente: cada fila se busca por su clave natural (siglas, codigo, nombre)
// y se crea si no existe o se actualiza si ya existe. Correrlo dos veces no duplica
// nada, y correrlo despues de corregir un archivo aplica la correccion.
@Component
@Profile("seed")
public class AcademicoSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AcademicoSeedRunner.class);

    private final UniversidadRepository universidadRepository;
    private final AreaRepository areaRepository;
    private final EsquemaCalificacionRepository esquemaCalificacionRepository;
    private final TemaRepository temaRepository;
    private final EstructuraExamenRepository estructuraExamenRepository;
    private final CarreraRepository carreraRepository;
    private final OfertaAcademicaRepository ofertaAcademicaRepository;

    public AcademicoSeedRunner(UniversidadRepository universidadRepository,
                                AreaRepository areaRepository,
                                EsquemaCalificacionRepository esquemaCalificacionRepository,
                                TemaRepository temaRepository,
                                EstructuraExamenRepository estructuraExamenRepository,
                                CarreraRepository carreraRepository,
                                OfertaAcademicaRepository ofertaAcademicaRepository) {
        this.universidadRepository = universidadRepository;
        this.areaRepository = areaRepository;
        this.esquemaCalificacionRepository = esquemaCalificacionRepository;
        this.temaRepository = temaRepository;
        this.estructuraExamenRepository = estructuraExamenRepository;
        this.carreraRepository = carreraRepository;
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
    }

    // Todo el seed va en una sola transaccion: si un archivo trae un dato invalido,
    // se revierte completo y no queda un catalogo cargado a medias.
    // El orden importa: cada archivo referencia filas que ya cargaron los anteriores.
    @Override
    @Transactional
    public void run(String... args) {
        sembrarUniversidades();
        sembrarAreas();
        sembrarEsquemas();
        sembrarTemas();
        sembrarEstructuraExamen();
        sembrarCarreras();
        sembrarOfertasAcademicas();
        log.info("Seed del catalogo academico terminado");
    }

    private void sembrarUniversidades() {
        for (Fila fila : leer("universidades.csv", 4)) {
            String siglas = fila.texto(0);
            Universidad universidad = universidadRepository.findBySiglas(siglas).orElseGet(Universidad::new);
            universidad.setSiglas(siglas);
            universidad.setNombre(fila.texto(1));
            universidad.setPuntajeMaximo(fila.entero(2));
            universidad.setTotalPreguntas(fila.entero(3));
            universidadRepository.save(universidad);
        }
    }

    private void sembrarAreas() {
        for (Fila fila : leer("areas.csv", 3)) {
            Universidad universidad = buscarUniversidad(fila, 0);
            String codigo = fila.texto(1);
            Area area = areaRepository.findByUniversidadIdAndCodigo(universidad.getId(), codigo)
                    .orElseGet(Area::new);
            area.setUniversidad(universidad);
            area.setCodigo(codigo);
            area.setNombre(fila.texto(2));
            areaRepository.save(area);
        }
    }

    private void sembrarEsquemas() {
        for (Fila fila : leer("esquemas.csv", 6)) {
            Universidad universidad = buscarUniversidad(fila, 0);
            String bloque = fila.texto(1);
            EsquemaCalificacion esquema = esquemaCalificacionRepository
                    .findByUniversidadIdAndNombreBloque(universidad.getId(), bloque)
                    .orElseGet(EsquemaCalificacion::new);
            esquema.setUniversidad(universidad);
            esquema.setNombreBloque(bloque);
            esquema.setValorAcierto(fila.decimal(2));
            esquema.setValorPenalidad(fila.decimal(3));
            esquema.setPuntajeMaximoBloque(fila.decimal(4));
            esquema.setOrden(fila.entero(5));
            esquemaCalificacionRepository.save(esquema);
        }
    }

    private void sembrarTemas() {
        for (Fila fila : leer("temas.csv", 3)) {
            String nombre = fila.texto(0);
            Tema tema = temaRepository.findByNombre(nombre).orElseGet(Tema::new);
            tema.setNombre(nombre);
            tema.setAreaConocimiento(areaConocimiento(fila, 1));
            tema.setTemario(fila.textoOpcional(2));
            temaRepository.save(tema);
        }
    }

    // La clave de cada fila es (area, tema), la misma pareja que protege la
    // restriccion unica uk_estructura_area_tema. El area y el bloque se buscan
    // dentro de la misma universidad de la fila, asi no se puede mezclar por
    // error un area de UNI con un bloque de calificacion de UNMSM.
    private void sembrarEstructuraExamen() {
        for (Fila fila : leer("estructura-examen.csv", 6)) {
            Universidad universidad = buscarUniversidad(fila, 0);
            Area area = buscarArea(fila, universidad, 1);
            EsquemaCalificacion esquema = buscarEsquema(fila, universidad, 2);
            Tema tema = buscarTema(fila, 3);

            EstructuraExamen estructura = estructuraExamenRepository
                    .findByAreaIdAndTemaId(area.getId(), tema.getId())
                    .orElseGet(EstructuraExamen::new);
            estructura.setArea(area);
            estructura.setEsquema(esquema);
            estructura.setTema(tema);
            estructura.setCantidadPreguntas(fila.entero(4));
            estructura.setOrden(fila.entero(5));
            estructuraExamenRepository.save(estructura);
        }
    }

    // Una sola fila por programa: si UNI y UNMSM tienen una carrera con el mismo
    // nombre oficial, comparten esta fila. Eso es lo que hace que OfertaAcademica
    // sea un M:N con atributos de verdad (la misma carrera con cortes distintos).
    private void sembrarCarreras() {
        for (Fila fila : leer("carreras.csv", 2)) {
            String nombre = fila.texto(0);
            Carrera carrera = carreraRepository.findByNombre(nombre).orElseGet(Carrera::new);
            carrera.setNombre(nombre);
            carrera.setFacultad(fila.texto(1));
            carreraRepository.save(carrera);
        }
    }

    // La clave es (universidad, carrera, area, proceso): la misma carrera puede
    // repetirse en otra universidad o en otro proceso de admision, con su propio
    // puntaje del ultimo ingresante.
    private void sembrarOfertasAcademicas() {
        for (Fila fila : leer("ofertas-2026-II.csv", 6)) {
            Universidad universidad = buscarUniversidad(fila, 0);
            Area area = buscarArea(fila, universidad, 1);
            Carrera carrera = buscarCarrera(fila, 2);
            String proceso = fila.texto(3);

            OfertaAcademica oferta = ofertaAcademicaRepository
                    .findByUniversidadIdAndCarreraIdAndAreaIdAndProcesoAdmision(
                            universidad.getId(), carrera.getId(), area.getId(), proceso)
                    .orElseGet(OfertaAcademica::new);
            oferta.setUniversidad(universidad);
            oferta.setCarrera(carrera);
            oferta.setArea(area);
            oferta.setProcesoAdmision(proceso);
            oferta.setPuntajeUltimoIngresante(fila.decimal(4));
            oferta.setVacantes(fila.entero(5));
            ofertaAcademicaRepository.save(oferta);
        }
    }

    private List<Fila> leer(String archivo, int columnasEsperadas) {
        List<Fila> filas = ArchivoSeed.leer(archivo, columnasEsperadas);
        log.info("{}: {} filas sincronizadas", archivo, filas.size());
        return filas;
    }

    private Universidad buscarUniversidad(Fila fila, int columna) {
        String siglas = fila.texto(columna);
        return universidadRepository.findBySiglas(siglas)
                .orElseThrow(() -> fila.error("no existe la universidad '" + siglas + "' en universidades.csv"));
    }

    private Area buscarArea(Fila fila, Universidad universidad, int columna) {
        String codigo = fila.texto(columna);
        return areaRepository.findByUniversidadIdAndCodigo(universidad.getId(), codigo)
                .orElseThrow(() -> fila.error("no existe el area '" + codigo + "' de "
                        + universidad.getSiglas() + " en areas.csv"));
    }

    private EsquemaCalificacion buscarEsquema(Fila fila, Universidad universidad, int columna) {
        String bloque = fila.texto(columna);
        return esquemaCalificacionRepository.findByUniversidadIdAndNombreBloque(universidad.getId(), bloque)
                .orElseThrow(() -> fila.error("no existe el bloque '" + bloque + "' de "
                        + universidad.getSiglas() + " en esquemas.csv"));
    }

    private Tema buscarTema(Fila fila, int columna) {
        String nombre = fila.texto(columna);
        return temaRepository.findByNombre(nombre)
                .orElseThrow(() -> fila.error("no existe el tema '" + nombre + "' en temas.csv"));
    }

    private Carrera buscarCarrera(Fila fila, int columna) {
        String nombre = fila.texto(columna);
        return carreraRepository.findByNombre(nombre)
                .orElseThrow(() -> fila.error("no existe la carrera '" + nombre + "' en carreras.csv"));
    }

    private AreaConocimiento areaConocimiento(Fila fila, int columna) {
        String valor = fila.texto(columna);
        try {
            return AreaConocimiento.valueOf(valor);
        } catch (IllegalArgumentException ex) {
            throw fila.error("area de conocimiento desconocida: '" + valor + "'");
        }
    }
}
