package com.rumbou.backend.client.gemini;

import com.rumbou.backend.entity.Dificultad;
import com.rumbou.backend.entity.OrigenPregunta;
import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.repository.PreguntaRepository;
import com.rumbou.backend.repository.TemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Solo corre con el profile "generar-preguntas". Cada tema es una llamada de
// generacion (las tres dificultades juntas) y, salvo --sin-validar, una de
// validacion: la cuota de Gemini se mide en llamadas, no en preguntas.
//
//   ./mvnw spring-boot:run -Dspring-boot.run.profiles=generar-preguntas
//      -Dspring-boot.run.arguments="--temaId=1 --dificultad=MEDIA --cantidad=10"
//   ./mvnw spring-boot:run -Dspring-boot.run.profiles=generar-preguntas
//      -Dspring-boot.run.arguments="--todos --cantidad=12 [--sin-validar]"
//
// Todo se guarda con aprobada=false: la revision humana sigue siendo obligatoria.
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

        Resultado resultado = generar(tema, dificultad, cantidad, false, validarConIa);
        log.info("Generacion terminada. Guardadas (pendientes de revision): {}. Rechazadas: {}",
                resultado.generadas(), resultado.rechazadas());
    }

    // Idempotente por par tema+dificultad: lo que ya tiene "cantidad" preguntas
    // no se vuelve a pedir, asi que cortar y relanzar no duplica ni gasta cuota.
    void generarTodos(int cantidad, boolean validarConIa) {
        List<Tema> temas = temaRepository.findAll();
        log.info("Modo --todos: {} temas, cantidad={} por dificultad, validacion con IA: {}",
                temas.size(), cantidad, validarConIa);

        int totalGeneradas = 0;
        int totalRechazadas = 0;
        int temasOmitidos = 0;

        for (Tema tema : temas) {
            Map<Dificultad, Integer> faltantes = new EnumMap<>(Dificultad.class);
            for (Dificultad dificultad : Dificultad.values()) {
                long existentes = preguntaRepository.countByTemaIdAndDificultad(tema.getId(), dificultad);
                if (existentes < cantidad) {
                    faltantes.put(dificultad, (int) (cantidad - existentes));
                }
            }
            if (faltantes.isEmpty()) {
                log.info("{}: ya tiene {} o mas por dificultad, se omite", tema.getNombre(), cantidad);
                temasOmitidos++;
                continue;
            }

            pausar();
            Map<Dificultad, List<PreguntaGeneradaDto>> generadasPorGemini;
            try {
                generadasPorGemini = geminiClient.generarPreguntasPorTema(tema.getNombre(), tema.getTemario(), faltantes);
            } catch (GeminiException ex) {
                // No cuenta como rechazada: el tema queda incompleto y la proxima corrida lo repite.
                log.warn("Error llamando a Gemini para {}, se omite este tema por ahora: {}",
                        tema.getNombre(), ex.getMessage());
                continue;
            }

            int generadasTema = 0;
            int rechazadasTema = 0;
            for (Map.Entry<Dificultad, List<PreguntaGeneradaDto>> entrada : generadasPorGemini.entrySet()) {
                Resultado resultado = validarYGuardar(tema, entrada.getKey(), entrada.getValue(), true, validarConIa);
                generadasTema += resultado.generadas();
                rechazadasTema += resultado.rechazadas();
            }

            totalGeneradas += generadasTema;
            totalRechazadas += rechazadasTema;
            log.info("Resumen {}: {} generadas, {} rechazadas", tema.getNombre(), generadasTema, rechazadasTema);
        }

        log.info("Generacion --todos terminada. Total guardadas: {}. Total rechazadas: {}. Temas omitidos: {}",
                totalGeneradas, totalRechazadas, temasOmitidos);
    }

    // Modo --temaId: una sola llamada para el par, sin pausa.
    private Resultado generar(Tema tema, Dificultad dificultad, int cantidad, boolean conPausa,
                              boolean validarConIa) {
        if (conPausa) {
            pausar();
        }

        List<PreguntaGeneradaDto> generadasPorGemini;
        try {
            generadasPorGemini = geminiClient.generarPreguntas(tema.getNombre(), tema.getTemario(), dificultad, cantidad);
        } catch (GeminiException ex) {
            // No cuenta como rechazada: el par queda incompleto y la proxima corrida lo reintenta.
            log.warn("Error llamando a Gemini para {} / {}, se omite este par por ahora: {}",
                    tema.getNombre(), dificultad, ex.getMessage());
            return new Resultado(0, 0);
        }

        return validarYGuardar(tema, dificultad, generadasPorGemini, conPausa, validarConIa);
    }

    private Resultado validarYGuardar(Tema tema, Dificultad dificultad, List<PreguntaGeneradaDto> generadasPorGemini,
                                      boolean conPausa, boolean validarConIa) {
        List<Optional<String>> rechazos;
        if (validarConIa) {
            // Una sola llamada de validacion por lote, no una por pregunta.
            if (conPausa) {
                pausar();
            }
            try {
                rechazos = validator.validarLote(generadasPorGemini);
            } catch (GeminiException ex) {
                // Si la validacion falla por cuota o red no se descarta el lote: se guarda
                // con la validacion estructural y queda para la revision humana.
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

    // Pausa para no pasar el limite por minuto; el reintento en 429 es la red de seguridad.
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
