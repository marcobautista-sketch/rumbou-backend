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
import java.util.Optional;

// Script fuera del runtime normal: solo corre con el profile "generar-preguntas".
// Cada par tema+dificultad son DOS llamadas a Gemini: una de generacion (pide
// las "cantidad" preguntas juntas, ver GeminiClient.generarPreguntas) y una de
// validacion (resuelve las N preguntas a ciegas, ver GeminiClient.resolverLote).
// Con --sin-validar se omite la segunda y queda solo la validacion estructural;
// la revision humana con /aprobar-lote sigue siendo obligatoria en ambos casos.
//
// Un solo tema (modo original, sin pausa: es una unica llamada de todas formas):
//   ./mvnw spring-boot:run -Dspring-boot.run.profiles=generar-preguntas
//      -Dspring-boot.run.arguments="--temaId=1 --dificultad=MEDIA --cantidad=10"
//
// Todos los temas de temas.csv x las 3 dificultades (22 x 3 = 66 pares, 132
// llamadas con validacion o 66 sin ella), con pausa entre llamadas para no
// pasar el limite por minuto de la cuota gratuita:
//   ./mvnw spring-boot:run -Dspring-boot.run.profiles=generar-preguntas
//      -Dspring-boot.run.arguments="--todos --cantidad=5 [--sin-validar]"
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
                                     @Value("${generador.pausa-entre-llamadas-ms:6500}") long pausaEntreLlamadasMs) {
        this.geminiClient = geminiClient;
        this.validator = validator;
        this.preguntaRepository = preguntaRepository;
        this.temaRepository = temaRepository;
        this.pausaEntreLlamadasMs = pausaEntreLlamadasMs;
    }

    @Override
    public void run(ApplicationArguments args) {
        int cantidad = Integer.parseInt(unicoValor(args, "cantidad"));

        boolean validarConIa = !args.containsOption("sin-validar");

        if (args.containsOption("todos")) {
            generarTodos(cantidad, validarConIa);
            return;
        }

        Long temaId = Long.valueOf(unicoValor(args, "temaId"));
        Dificultad dificultad = Dificultad.valueOf(unicoValor(args, "dificultad"));
        Tema tema = temaRepository.findById(temaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe un tema con id " + temaId));

        // Sin pausa: es el modo original, para generar un puñado de preguntas a mano.
        Resultado resultado = generar(tema, dificultad, cantidad, false, validarConIa);
        log.info("Generacion terminada. Guardadas (pendientes de revision): {}. Rechazadas: {}",
                resultado.generadas(), resultado.rechazadas());
    }

    // Recorre todos los temas x las 3 dificultades. Idempotente: si un par ya
    // tiene "cantidad" o mas preguntas (aprobadas o no), lo salta - asi cortar y
    // volver a correr el mismo comando no duplica ni vuelve a gastar cuota de
    // Gemini en lo que ya estaba listo.
    void generarTodos(int cantidad, boolean validarConIa) {
        List<Tema> temas = temaRepository.findAll();
        log.info("Modo --todos: {} temas x {} dificultades, cantidad={} por par, validacion con IA: {}",
                temas.size(), Dificultad.values().length, cantidad, validarConIa);

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

                Resultado resultado = generar(tema, dificultad, cantidad, true, validarConIa);
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

    // Nucleo compartido por ambos modos: una sola llamada a Gemini pide las
    // "cantidad" preguntas del par de una vez (ver GeminiClient.generarPreguntas).
    // conPausa solo se activa en --todos: el modo --temaId original no la
    // necesita, ya que hace una unica llamada de todas formas.
    private Resultado generar(Tema tema, Dificultad dificultad, int cantidad, boolean conPausa,
                              boolean validarConIa) {
        if (conPausa) {
            pausar();
        }

        List<PreguntaGeneradaDto> generadasPorGemini;
        try {
            generadasPorGemini = geminiClient.generarPreguntas(tema.getNombre(), tema.getTemario(), dificultad, cantidad);
        } catch (GeminiException ex) {
            // No se cuenta como "rechazada" (eso es del validador): el par se queda
            // por debajo de "cantidad" y una proxima corrida lo va a reintentar solo,
            // gracias al salteo idempotente de generarTodos().
            log.warn("Error llamando a Gemini para {} / {}, se omite este par por ahora: {}",
                    tema.getNombre(), dificultad, ex.getMessage());
            return new Resultado(0, 0);
        }

        List<Optional<String>> rechazos;
        if (validarConIa) {
            // Una sola llamada de validacion para todo el lote (no una por
            // pregunta), con su pausa: es la segunda llamada del par.
            if (conPausa) {
                pausar();
            }
            try {
                rechazos = validator.validarLote(generadasPorGemini);
            } catch (GeminiException ex) {
                // Si la validacion falla por cuota/red no se descarta el lote: se
                // guarda con la validacion estructural y queda para la revision humana.
                log.warn("No se pudo validar con IA el lote {} / {}, se guarda solo con validacion estructural: {}",
                        tema.getNombre(), dificultad, ex.getMessage());
                rechazos = generadasPorGemini.stream().map(validator::validarEstructura).toList();
            }
        } else {
            rechazos = generadasPorGemini.stream().map(validator::validarEstructura).toList();
        }

        int generadas = 0;
        int rechazadas = 0;
        for (int i = 0; i < generadasPorGemini.size(); i++) {
            PreguntaGeneradaDto generada = generadasPorGemini.get(i);
            Optional<String> rechazo = rechazos.get(i);
            if (rechazo.isPresent()) {
                rechazadas++;
                log.info("Pregunta rechazada ({}): {}", rechazo.get(), generada.enunciado());
                continue;
            }

            guardar(tema, dificultad, generada);
            generadas++;
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
