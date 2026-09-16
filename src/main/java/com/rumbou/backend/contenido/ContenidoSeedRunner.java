package com.rumbou.backend.contenido;

import com.rumbou.backend.academico.Tema;
import com.rumbou.backend.academico.TemaRepository;
import com.rumbou.backend.contenido.ArchivoSeed.Fila;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Carga el banco de preguntas aprobadas desde src/main/resources/seed/preguntas.csv.
// Mismo patron del equipo que AcademicoSeedRunner/GamificacionSeedRunner:
// @Profile("seed"), una sola transaccion, idempotente por clave natural, sin data.sql.
//
// El archivo lo genera ExportadorPreguntasRunner (profile "exportar-preguntas") a
// partir de las preguntas ya aprobadas en una base local: asi produccion tiene el
// banco de preguntas cargado sin volver a llamar a Gemini.
//   ./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
@Component
@Profile("seed")
public class ContenidoSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ContenidoSeedRunner.class);

    private final PreguntaRepository preguntaRepository;
    private final TemaRepository temaRepository;

    public ContenidoSeedRunner(PreguntaRepository preguntaRepository, TemaRepository temaRepository) {
        this.preguntaRepository = preguntaRepository;
        this.temaRepository = temaRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        sembrarPreguntas();
        log.info("Seed del banco de preguntas terminado");
    }

    // La clave natural es (tema, enunciado). Todas las preguntas de este archivo
    // se guardan como aprobadas: si no lo estaban, no deberian haberse exportado.
    private void sembrarPreguntas() {
        for (Fila fila : leer("preguntas.csv", 11)) {
            Tema tema = buscarTema(fila, 0);
            String enunciado = fila.texto(2);

            Pregunta pregunta = preguntaRepository.findByTemaIdAndEnunciado(tema.getId(), enunciado)
                    .orElseGet(Pregunta::new);
            pregunta.setTema(tema);
            pregunta.setEnunciado(enunciado);
            pregunta.setAlternativas(List.of(
                    fila.texto(3), fila.texto(4), fila.texto(5), fila.texto(6), fila.texto(7)));
            pregunta.setClaveCorrecta(fila.entero(8));
            pregunta.setExplicacion(fila.textoOpcional(9));
            pregunta.setDificultad(dificultad(fila, 1));
            pregunta.setOrigen(origen(fila, 10));
            pregunta.setAprobada(true);
            preguntaRepository.save(pregunta);
        }
    }

    private List<Fila> leer(String archivo, int columnasEsperadas) {
        List<Fila> filas = ArchivoSeed.leer(archivo, columnasEsperadas);
        log.info("{}: {} filas sincronizadas", archivo, filas.size());
        return filas;
    }

    private Tema buscarTema(Fila fila, int columna) {
        String nombre = fila.texto(columna);
        return temaRepository.findByNombre(nombre)
                .orElseThrow(() -> fila.error("no existe el tema '" + nombre + "' en temas.csv"));
    }

    private Dificultad dificultad(Fila fila, int columna) {
        String valor = fila.texto(columna);
        try {
            return Dificultad.valueOf(valor);
        } catch (IllegalArgumentException ex) {
            throw fila.error("dificultad desconocida: '" + valor + "'");
        }
    }

    private OrigenPregunta origen(Fila fila, int columna) {
        String valor = fila.texto(columna);
        try {
            return OrigenPregunta.valueOf(valor);
        } catch (IllegalArgumentException ex) {
            throw fila.error("origen desconocido: '" + valor + "'");
        }
    }
}
