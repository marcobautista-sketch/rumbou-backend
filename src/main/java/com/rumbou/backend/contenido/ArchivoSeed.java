package com.rumbou.backend.contenido;

import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// Lee los archivos de src/main/resources/seed/. Vive separado del runner para que
// el test de consistencia de los archivos use exactamente las mismas reglas de
// lectura que el seed real, en vez de repetirlas.
//
// Formato: UTF-8 y separador "|". Las lineas vacias y las que empiezan con "#" se
// ignoran, y la primera fila restante es el encabezado.
//
// Copia deliberada de academico/ArchivoSeed y gamificacion/ArchivoSeed: unificarlas
// en shared/seed/ es la tarea #41 de Juan Carlos, no se toca desde aqui.
final class ArchivoSeed {

    private static final String CARPETA_SEED = "seed/";
    private static final String SEPARADOR = "\\|";
    // Excel y algunos editores de Windows agregan esta marca invisible al inicio del archivo.
    private static final String MARCA_BOM = "﻿";

    private ArchivoSeed() {
    }

    // Devuelve las filas de datos del archivo y valida que cada una tenga la
    // cantidad de columnas esperada.
    static List<Fila> leer(String archivo, int columnasEsperadas) {
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

        return filas;
    }

    // Una fila de un archivo de seed. Guarda de donde salio para que los errores
    // digan exactamente que archivo y que linea corregir.
    record Fila(String archivo, int numeroLinea, String[] columnas) {

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
