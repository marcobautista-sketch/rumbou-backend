package com.rumbou.backend.contenido.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rumbou.backend.contenido.Dificultad;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// Cliente para la API de Gemini (Google AI Studio). No agrega dependencia nueva:
// usa java.net.http.HttpClient y Jackson, que ya vienen con Spring Boot.
@Component
public class GeminiClient {

    private static final String MODEL = "gemini-2.5-flash";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
            + MODEL + ":generateContent";

    // La capa gratuita de Gemini limita solicitudes por minuto: un 429 es
    // transitorio, no un error real, asi que conviene esperar y reintentar en
    // vez de perder la pregunta completa (importa sobre todo generando en lote).
    private static final int MAX_INTENTOS_POR_429 = 3;
    private static final Duration ESPERA_BASE_429 = Duration.ofSeconds(2);

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(@Value("${gemini.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public PreguntaGeneradaDto generarPregunta(String temaNombre, String temario, Dificultad dificultad) {
        String temarioTexto = (temario == null || temario.isBlank())
                ? ""
                : "\n\nLimita la pregunta al siguiente temario oficial del tema "
                        + "(no salgas de estos subtemas):\n" + temario + "\n";

        String prompt = """
                Genera una pregunta de opcion multiple, estilo examen de admision universitaria \
                peruano (UNI/UNMSM), sobre el tema "%s", con dificultad %s.%s

                No copies textualmente preguntas de examenes oficiales reales: usalas solo como \
                referencia de estilo y nivel de dificultad. La pregunta debe ser original.

                Debe tener exactamente 5 alternativas, con una unica respuesta correcta. \
                Incluye una explicacion breve de por que esa alternativa es la correcta.
                """.formatted(temaNombre, dificultad.name(), temarioTexto);

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "OBJECT");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("enunciado").put("type", "STRING");
        ObjectNode alternativas = properties.putObject("alternativas");
        alternativas.put("type", "ARRAY");
        alternativas.putObject("items").put("type", "STRING");
        properties.putObject("claveCorrecta").put("type", "INTEGER");
        properties.putObject("explicacion").put("type", "STRING");
        schema.putArray("required").add("enunciado").add("alternativas").add("claveCorrecta").add("explicacion");

        JsonNode respuesta = llamar(prompt, schema);

        return objectMapper.convertValue(respuesta, PreguntaGeneradaDto.class);
    }

    // Segunda llamada de validacion: no le decimos cual es la clave, y comparamos
    // su respuesta contra claveCorrecta en GeminiPreguntaValidator.
    public int resolver(String enunciado, java.util.List<String> alternativas) {
        StringBuilder prompt = new StringBuilder("Resuelve la siguiente pregunta de opcion multiple ");
        prompt.append("y responde unicamente con el indice (0 a 4) de la alternativa correcta.\n\n");
        prompt.append(enunciado).append("\n");
        for (int i = 0; i < alternativas.size(); i++) {
            prompt.append(i).append(") ").append(alternativas.get(i)).append("\n");
        }

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "OBJECT");
        schema.putObject("properties").putObject("claveElegida").put("type", "INTEGER");
        schema.putArray("required").add("claveElegida");

        JsonNode respuesta = llamar(prompt.toString(), schema);
        return respuesta.path("claveElegida").asInt(-1);
    }

    // A diferencia de resolver(), aqui si le damos la clave: el objetivo es explicar, no validar.
    public String explicar(String enunciado, java.util.List<String> alternativas, int claveCorrecta) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Explica en un parrafo breve, para un postulante que respondio mal esta pregunta ");
        prompt.append("de un examen de admision, por que la alternativa correcta es la correcta.\n\n");
        prompt.append(enunciado).append("\n");
        for (int i = 0; i < alternativas.size(); i++) {
            prompt.append(i).append(") ").append(alternativas.get(i)).append("\n");
        }
        prompt.append("\nLa alternativa correcta es la numero ").append(claveCorrecta).append(".");

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "OBJECT");
        schema.putObject("properties").putObject("explicacion").put("type", "STRING");
        schema.putArray("required").add("explicacion");

        JsonNode respuesta = llamar(prompt.toString(), schema);
        String explicacion = respuesta.path("explicacion").asText();
        if (explicacion.isBlank()) {
            // Si dejamos pasar esto, PreguntaService le cobraria la consulta diaria
            // al usuario PRO a cambio de una explicacion vacia.
            throw new GeminiException("Gemini devolvio una explicacion vacia");
        }
        return explicacion;
    }

    private JsonNode llamar(String prompt, ObjectNode responseSchema) {
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode content = body.putArray("contents").addObject();
        content.putArray("parts").addObject().put("text", prompt);

        ObjectNode generationConfig = body.putObject("generationConfig");
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.set("responseSchema", responseSchema);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
        } catch (IOException ex) {
            throw new GeminiException("Error armando la solicitud a Gemini", ex);
        }

        HttpResponse<String> response = enviarConReintento(request);

        JsonNode root = leerJson(response.body());
        String textoJson = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        return leerJson(textoJson);
    }

    // Un 429 se reintenta con espera creciente; cualquier otro codigo distinto
    // de 200 (o un 429 que ya agoto los intentos) se trata como fallo definitivo.
    // while(true) en vez de un for: asi el mensaje de "agoto los intentos" es
    // alcanzable de verdad, en vez de quedar detras del chequeo generico de abajo.
    private HttpResponse<String> enviarConReintento(HttpRequest request) {
        int intento = 1;
        while (true) {
            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new GeminiException("Error llamando a la API de Gemini", ex);
            }

            if (response.statusCode() == 200) {
                return response;
            }
            if (response.statusCode() != 429) {
                throw new GeminiException("Gemini respondio " + response.statusCode() + ": " + response.body());
            }
            if (intento >= MAX_INTENTOS_POR_429) {
                throw new GeminiException("Gemini sigue respondiendo 429 despues de " + MAX_INTENTOS_POR_429 + " intentos");
            }
            esperarAntesDeReintentar(intento);
            intento++;
        }
    }

    private void esperarAntesDeReintentar(int intento) {
        try {
            Thread.sleep(ESPERA_BASE_429.toMillis() * intento);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GeminiException("Interrumpido esperando para reintentar la llamada a Gemini", ex);
        }
    }

    private JsonNode leerJson(String texto) {
        try {
            return objectMapper.readTree(texto);
        } catch (IOException ex) {
            throw new GeminiException("Gemini devolvio una respuesta que no se pudo interpretar", ex);
        }
    }
}
