package com.rumbou.backend.academico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// Carga el catalogo academico desde los archivos de src/main/resources/seed/.
// Solo corre con el profile "seed" (decision del equipo: un runner por paquete,
// cada uno dueno de sembrar solo sus propias tablas, y nada de data.sql):
//   ./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
//
// Ningun valor del examen esta escrito aqui: este runner solo lee archivos y los
// guarda. Corregir un puntaje o agregar un tema es editar un archivo, no el codigo.
//
// Es idempotente: cada fila se busca por su clave natural (siglas, codigo, nombre)
// y se crea si no existe o se actualiza si ya existe. Correrlo dos veces no duplica
// nada, y correrlo despues de corregir un archivo aplica la correccion.
@Component
@Profile("seed")
public class AcademicoSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AcademicoSeedRunner.class);

    private static final String CARPETA_SEED = "seed/";
    private static final String SEPARADOR = "\\|";
    // Excel y algunos editores de Windows agregan esta marca invisible al inicio del archivo.
    private static final String MARCA_BOM = "﻿";

    private final UniversidadRepository universidadRepository;
    private final AreaRepository areaRepository;
    private final EsquemaCalificacionRepository esquemaCalificacionRepository;
    private final TemaRepository temaRepository;

    public AcademicoSeedRunner(UniversidadRepository universidadRepository,
                                AreaRepository areaRepository,
                                EsquemaCalificacionRepository esquemaCalificacionRepository,
                                TemaRepository temaRepository) {
        this.universidadRepository = universidadRepository;
        this.areaRepository = areaRepository;
        this.esquemaCalificacionRepository = esquemaCalificacionRepository;
        this.temaRepository = temaRepository;
    }

    // Todo el seed va en una sola transaccion: si un archivo trae un dato invalido,
    // se revierte completo y no queda un catalogo cargado a medias.
    @Override
    @Transactional
    public void run(String... args) {
        sembrarUniversidades();
        sembrarAreas();
        sembrarEsquemas();
        sembrarTemas();
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

    private Universidad buscarUniversidad(Fila fila, int columna) {
        String siglas = fila.texto(columna);
        return universidadRepository.findBySiglas(siglas)
                .orElseThrow(() -> fila.error("no existe la universidad '" + siglas + "' en universidades.csv"));
    }

    private AreaConocimiento areaConocimiento(Fila fila, int columna) {
        String valor = fila.texto(columna);
        try {
            return AreaConocimiento.valueOf(valor);
        } catch (IllegalArgumentException ex) {
            throw fila.error("area de conocimiento desconocida: '" + valor + "'");
        }
    }

    // Lee un archivo de seed: ignora lineas vacias y comentarios (#), salta el
    // encabezado y valida que cada fila tenga la cantidad de columnas esperada.
    private List<Fila> leer(String archivo, int columnasEsperadas) {
        ClassPathResource recurso = new ClassPathResource(CARPETA_SEED + archivo);
        List<Fila> filas = new ArrayList<>();

        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(recurso.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            int numeroLinea = 0;
            boolean encabezadoLeido = false;

            while ((linea = lector.readLine()) != null) {
                numeroLinea++;
                if (numeroLinea == 1 && linea.startsWith(MARCA_BOM)) {
                    linea = linea.substring(1);
                }
                if (linea.isBlank() || linea.startsWith("#")) {
                    continue;
                }
                if (!encabezadoLeido) {
                    encabezadoLeido = true;
                    continue;
                }

                Fila fila = new Fila(archivo, numeroLinea, linea.split(SEPARADOR, -1));
                if (fila.columnas().length != columnasEsperadas) {
                    throw fila.error("se esperaban " + columnasEsperadas + " columnas y hay "
                            + fila.columnas().length);
                }
                filas.add(fila);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer el archivo de seed " + archivo, ex);
        }

        log.info("{}: {} filas sincronizadas", archivo, filas.size());
        return filas;
    }

    // Una fila de un archivo de seed. Guarda de donde salio para que los errores
    // digan exactamente que archivo y que linea corregir.
    private record Fila(String archivo, int numeroLinea, String[] columnas) {

        String texto(int indice) {
            String valor = columnas[indice].trim();
            if (valor.isEmpty()) {
                throw error("la columna " + (indice + 1) + " no puede estar vacia");
            }
            return valor;
        }

        String textoOpcional(int indice) {
            String valor = columnas[indice].trim();
            return valor.isEmpty() ? null : valor;
        }

        int entero(int indice) {
            String valor = texto(indice);
            try {
                return Integer.parseInt(valor);
            } catch (NumberFormatException ex) {
                throw error("'" + valor + "' no es un numero entero");
            }
        }

        double decimal(int indice) {
            String valor = texto(indice);
            try {
                return Double.parseDouble(valor);
            } catch (NumberFormatException ex) {
                throw error("'" + valor + "' no es un numero valido (usa punto decimal, no coma)");
            }
        }

        IllegalStateException error(String mensaje) {
            return new IllegalStateException(archivo + ", linea " + numeroLinea + ": " + mensaje);
        }
    }
}
