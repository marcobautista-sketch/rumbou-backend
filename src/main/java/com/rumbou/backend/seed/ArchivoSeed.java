package com.rumbou.backend.seed;

import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// Lector de los archivos de seed (UTF-8, separador "|"; se ignoran las lineas
// vacias y las que empiezan con "#"; la primera restante es el encabezado).
// Lo usan el seed real y los tests de consistencia, con las mismas reglas.
final class ArchivoSeed {

    private static final String CARPETA_SEED = "seed/";
    private static final String SEPARADOR = "\\|";
    // BOM que agregan Excel y algunos editores de Windows.
    private static final String MARCA_BOM = "﻿";

    private ArchivoSeed() {
    }

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

    // Fila de un archivo de seed; recuerda archivo y linea para que los errores apunten al lugar exacto.
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
