package com.rumbou.backend.contenido.gemini;

import com.rumbou.backend.academico.Tema;
import com.rumbou.backend.academico.TemaRepository;
import com.rumbou.backend.contenido.Dificultad;
import com.rumbou.backend.contenido.OrigenPregunta;
import com.rumbou.backend.contenido.Pregunta;
import com.rumbou.backend.contenido.PreguntaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Script fuera del runtime normal: solo corre con el profile "generar-preguntas".
//
// Un solo tema (modo original, sin pausa entre llamadas):
//   ./mvnw spring-boot:run -Dspring-boot.run.profiles=generar-preguntas
//      -Dspring-boot.run.arguments="--temaId=1 --dificultad=MEDIA --cantidad=10"
//
// Todos los temas de temas.csv x las 3 dificultades, con pausa entre llamadas:
//   ./mvnw spring-boot:run -Dspring-boot.run.profiles=generar-preguntas
//      -Dspring-boot.run.arguments="--todos --cantidad=5"
//
// Guarda todo con aprobada=false, pendiente de revision humana.
@Component
@Profile("generar-preguntas")
public class GeneradorPreguntasRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GeneradorPreguntasRunner.class);

    private final GeminiClient geminiClient;
    private final GeminiPreguntaValidator validator;
    private final PreguntaRepository preguntaRepository;
    private final TemaRepository temaRepository;
    private final long pausaEntreLlamadasMs;

    public GeneradorPreguntasRunner(GeminiClient geminiClient,
                                     GeminiPreguntaValidator validator,
                                     PreguntaRepository preguntaRepository,
                                     TemaRepository temaRepository,
                                     @Value("${generador.pausa-entre-llamadas-ms:500}") long pausaEntreLlamadasMs) {
        this.geminiClient = geminiClient;
        this.validator = validator;
        this.preguntaRepository = preguntaRepository;
        this.temaRepository = temaRepository;
        this.pausaEntreLlamadasMs = pausaEntreLlamadasMs;
    }

    @Override
    public void run(ApplicationArguments args) {
        int cantidad = Integer.parseInt(unicoValor(args, "cantidad"));

        if (args.containsOption("todos")) {
            generarTodos(cantidad);
            return;
        }

        Long temaId = Long.valueOf(unicoValor(args, "temaId"));
        Dificultad dificultad = Dificultad.valueOf(unicoValor(args, "dificultad"));
        Tema tema = temaRepository.findById(temaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe un tema con id " + temaId));

        // Sin pausa: es el modo original, para generar un puñado de preguntas a mano.
        Resultado resultado = generar(tema, dificultad, cantidad, false);
        log.info("Generacion terminada. Guardadas (pendientes de revision): {}. Rechazadas: {}",
                resultado.generadas(), resultado.rechazadas());
    }

    // Recorre todos los temas x las 3 dificultades. Idempotente: si un par ya
    // tiene "cantidad" o mas preguntas (aprobadas o no), lo salta - asi cortar y
    // volver a correr el mismo comando no duplica ni vuelve a gastar cuota de
    // Gemini en lo que ya estaba listo.
    void generarTodos(int cantidad) {
        List<Tema> temas = temaRepository.findAll();
        log.info("Modo --todos: {} temas x {} dificultades, cantidad={} por par",
                temas.size(), Dificultad.values().length, cantidad);

        int totalGeneradas = 0;
        int totalRechazadas = 0;
        int paresOmitidos = 0;

        for (Tema tema : temas) {
            int generadasTema = 0;
            int rechazadasTema = 0;

            for (Dificultad dificultad : Dificultad.values()) {
                long existentes = preguntaRepository.countByTemaIdAndDificultad(tema.getId(), dificultad);
                if (existentes >= cantidad) {
                    log.info("{} / {}: ya tiene {} (>= {}), se omite", tema.getNombre(), dificultad, existentes, cantidad);
                    paresOmitidos++;
                    continue;
                }

                Resultado resultado = generar(tema, dificultad, cantidad, true);
                generadasTema += resultado.generadas();
                rechazadasTema += resultado.rechazadas();
            }

            totalGeneradas += generadasTema;
            totalRechazadas += rechazadasTema;
            log.info("Resumen {}: {} generadas, {} rechazadas", tema.getNombre(), generadasTema, rechazadasTema);
        }

        log.info("Generacion --todos terminada. Total guardadas: {}. Total rechazadas: {}. Pares omitidos: {}",
                totalGeneradas, totalRechazadas, paresOmitidos);
    }

    // Nucleo compartido por ambos modos. conPausa solo se activa en --todos: el
    // modo --temaId original no la necesita y no cambia su comportamiento.
    private Resultado generar(Tema tema, Dificultad dificultad, int cantidad, boolean conPausa) {
        int generadas = 0;
        int rechazadas = 0;

        for (int i = 0; i < cantidad; i++) {
            if (conPausa) {
                pausar();
            }
            try {
                PreguntaGeneradaDto generada = geminiClient.generarPregunta(
                        tema.getNombre(), tema.getTemario(), dificultad);

                var rechazo = validator.validar(generada);
                if (rechazo.isPresent()) {
                    rechazadas++;
                    log.info("Pregunta rechazada ({}): {}", rechazo.get(), generada.enunciado());
                    continue;
                }

                guardar(tema, dificultad, generada);
                generadas++;
            } catch (GeminiException ex) {
                rechazadas++;
                log.warn("Error llamando a Gemini, se omite esta pregunta: {}", ex.getMessage());
            }
        }

        return new Resultado(generadas, rechazadas);
    }

    // Pausa entre llamadas para no acercarse al limite por minuto de la capa
    // gratuita de Gemini. El reintento en 429 (dentro de GeminiClient) sigue
    // siendo la red de seguridad si aun asi se dispara el limite.
    private void pausar() {
        try {
            Thread.sleep(pausaEntreLlamadasMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    void guardar(Tema tema, Dificultad dificultad, PreguntaGeneradaDto generada) {
        Pregunta pregunta = new Pregunta(
                tema,
                generada.enunciado(),
                generada.alternativas(),
                generada.claveCorrecta(),
                generada.explicacion(),
                dificultad,
                OrigenPregunta.IA_APROBADA,
                false
        );
        preguntaRepository.save(pregunta);
    }

    private String unicoValor(ApplicationArguments args, String nombre) {
        if (!args.containsOption(nombre) || args.getOptionValues(nombre).isEmpty()) {
            throw new IllegalArgumentException("Falta el argumento --" + nombre);
        }
        return args.getOptionValues(nombre).get(0);
    }

    private record Resultado(int generadas, int rechazadas) {
    }
}
