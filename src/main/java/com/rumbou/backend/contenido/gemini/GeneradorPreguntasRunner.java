package com.rumbou.backend.contenido.gemini;

import com.rumbou.backend.academico.Tema;
import com.rumbou.backend.contenido.Dificultad;
import com.rumbou.backend.contenido.OrigenPregunta;
import com.rumbou.backend.contenido.Pregunta;
import com.rumbou.backend.contenido.PreguntaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Script de generacion de preguntas con IA, fuera del runtime normal de la app:
// solo corre si se activa el profile "generar-preguntas", nunca en produccion ni en dev.
//
// Uso (con Docker/Postgres local levantado):
//   ./mvnw spring-boot:run -Dspring-boot.run.profiles=generar-preguntas \
//       -Dspring-boot.run.arguments="--temaId=1 --dificultad=MEDIA --cantidad=10"
//
// Requiere la variable de entorno GEMINI_API_KEY (ver GeminiClient). Cada pregunta
// se guarda con aprobada=false: la meta de 450 preguntas se completa con revision
// humana en el panel de administracion antes de marcarlas aprobadas (regla del
// CLAUDE.md: no hardcodear, y no publicar contenido de IA sin revisar).
@Component
@Profile("generar-preguntas")
public class GeneradorPreguntasRunner implements ApplicationRunner {

    private final GeminiClient geminiClient;
    private final GeminiPreguntaValidator validator;
    private final PreguntaRepository preguntaRepository;
    private final EntityManager entityManager;

    public GeneradorPreguntasRunner(GeminiClient geminiClient,
                                     GeminiPreguntaValidator validator,
                                     PreguntaRepository preguntaRepository,
                                     EntityManager entityManager) {
        this.geminiClient = geminiClient;
        this.validator = validator;
        this.preguntaRepository = preguntaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long temaId = Long.valueOf(unicoValor(args, "temaId"));
        Dificultad dificultad = Dificultad.valueOf(unicoValor(args, "dificultad"));
        int cantidad = Integer.parseInt(unicoValor(args, "cantidad"));

        Tema tema = entityManager.find(Tema.class, temaId);
        if (tema == null) {
            throw new IllegalArgumentException("No existe un tema con id " + temaId);
        }

        int generadas = 0;
        int rechazadas = 0;

        for (int i = 0; i < cantidad; i++) {
            try {
                PreguntaGeneradaDto generada = geminiClient.generarPregunta(tema.getNombre(), dificultad);

                var rechazo = validator.validar(generada);
                if (rechazo.isPresent()) {
                    rechazadas++;
                    System.out.println("Pregunta rechazada (" + rechazo.get() + "): " + generada.enunciado());
                    continue;
                }

                guardar(tema, dificultad, generada);
                generadas++;
            } catch (GeminiException ex) {
                rechazadas++;
                System.out.println("Error llamando a Gemini, se omite esta pregunta: " + ex.getMessage());
            }
        }

        System.out.println("Generacion terminada. Guardadas (pendientes de revision): " + generadas
                + ". Rechazadas: " + rechazadas);
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
}
