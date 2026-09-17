package com.rumbou.backend.seed;

import com.rumbou.backend.entity.Logro;
import com.rumbou.backend.entity.TipoLogro;
import com.rumbou.backend.repository.LogroRepository;
import com.rumbou.backend.seed.ArchivoSeed.Fila;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Siembra el catalogo de logros desde src/main/resources/seed/logros.csv.
// Mismo patron del equipo que AcademicoSeedRunner: @Profile("seed"), una sola
// transaccion, idempotente por clave natural (nombre), sin data.sql.
// Sin @Order, Spring no garantiza en que orden corren los runners: en una base
// vacia el seed de preguntas llego a correr antes que el de temas y fallo.
// Despues del catalogo academico.
@Order(2)
@Component
@Profile("seed")
public class GamificacionSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GamificacionSeedRunner.class);

    private final LogroRepository logroRepository;

    public GamificacionSeedRunner(LogroRepository logroRepository) {
        this.logroRepository = logroRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        sembrarLogros();
        log.info("Seed del catalogo de logros terminado");
    }

    private void sembrarLogros() {
        for (Fila fila : leer("logros.csv", 4)) {
            String nombre = fila.texto(0);
            Logro logro = logroRepository.findByNombre(nombre).orElseGet(Logro::new);
            logro.setNombre(nombre);
            logro.setDescripcion(fila.textoOpcional(1));
            logro.setCondicion(tipoLogro(fila, 2));
            logro.setValorRequerido(fila.entero(3));
            logroRepository.save(logro);
        }
    }

    private List<Fila> leer(String archivo, int columnasEsperadas) {
        List<Fila> filas = ArchivoSeed.leer(archivo, columnasEsperadas);
        log.info("{}: {} filas sincronizadas", archivo, filas.size());
        return filas;
    }

    private TipoLogro tipoLogro(Fila fila, int columna) {
        String valor = fila.texto(columna);
        try {
            return TipoLogro.valueOf(valor);
        } catch (IllegalArgumentException ex) {
            throw fila.error("tipo de logro desconocido: '" + valor + "'");
        }
    }
}