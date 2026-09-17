package com.rumbou.backend.seed;

import com.rumbou.backend.entity.Pregunta;
import com.rumbou.backend.repository.PreguntaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

// Vuelca las preguntas aprobadas de la base local a src/main/resources/seed/preguntas.csv
// (el formato que lee ContenidoSeedRunner). Sobreescribe el archivo completo.
//   ./mvnw spring-boot:run -Dspring-boot.run.profiles=exportar-preguntas
@Component
@Profile("exportar-preguntas")
public class ExportadorPreguntasRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExportadorPreguntasRunner.class);
    private static final Path ARCHIVO_SALIDA = Path.of("src/main/resources/seed/preguntas.csv");
    private static final String ENCABEZADO = "tema|dificultad|enunciado|alternativa1|alternativa2"
            + "|alternativa3|alternativa4|alternativa5|claveCorrecta|explicacion|origen";

    private final PreguntaRepository preguntaRepository;

    public ExportadorPreguntasRunner(PreguntaRepository preguntaRepository) {
        this.preguntaRepository = preguntaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        List<Pregunta> aprobadas = preguntaRepository.findByAprobadaTrueConTema().stream()
                .sorted(Comparator.<Pregunta, String>comparing(p -> p.getTema().getNombre())
                        .thenComparing(Pregunta::getDificultad))
                .toList();

        try (BufferedWriter escritor = Files.newBufferedWriter(ARCHIVO_SALIDA, StandardCharsets.UTF_8)) {
            escritor.write(ENCABEZADO);
            escritor.newLine();
            for (Pregunta pregunta : aprobadas) {
                escritor.write(aFila(pregunta));
                escritor.newLine();
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo escribir " + ARCHIVO_SALIDA, ex);
        }

        log.info("{}: {} preguntas aprobadas exportadas", ARCHIVO_SALIDA, aprobadas.size());
    }

    private String aFila(Pregunta pregunta) {
        List<String> alternativas = pregunta.getAlternativas();
        return String.join("|",
                sanear(pregunta.getTema().getNombre()),
                pregunta.getDificultad().name(),
                sanear(pregunta.getEnunciado()),
                sanear(alternativas.get(0)),
                sanear(alternativas.get(1)),
                sanear(alternativas.get(2)),
                sanear(alternativas.get(3)),
                sanear(alternativas.get(4)),
                String.valueOf(pregunta.getClaveCorrecta()),
                sanear(pregunta.getExplicacion()),
                pregunta.getOrigen().name());
    }

    // El separador es "|" y cada fila es una linea: se normalizan barras y saltos de linea.
    private String sanear(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("\r", " ").replace("\n", " ").replace("|", "/").trim();
    }
}
