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

// Cliente minimo para la API de Gemini (Google AI Studio), usado solo por el script
// de generacion de preguntas (ver GeneradorPreguntasRunner). No usa ningun SDK nuevo:
// java.net.http.HttpClient y Jackson ya vienen con Spring Boot, asi que no hizo falta
// agregar una dependencia al pom.xml para esto.
//
// La API key SIEMPRE viene de una variable de entorno (GEMINI_API_KEY), nunca del
// repositorio (ver application.properties: gemini.api-key=${GEMINI_API_KEY:}).
@Component
public class GeminiClient {

    private static final String MODEL = "gemini-2.5-flash";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
            + MODEL + ":generateContent";

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

    // Le pide a Gemini una pregunta nueva de opcion multiple sobre un tema y dificultad
    // dados. Explicitamente le decimos que NO copie preguntas textuales de examenes
    // oficiales, solo que las use como referencia de estilo (regla del CLAUDE.md).
    public PreguntaGeneradaDto generarPregunta(String temaNombre, Dificultad dificultad) {
        String prompt = """
                Genera una pregunta de opcion multiple, estilo examen de admision universitaria \
                peruano (UNI/UNMSM), sobre el tema "%s", con dificultad %s.

                No copies textualmente preguntas de examenes oficiales reales: usalas solo como \
                referencia de estilo y nivel de dificultad. La pregunta debe ser original.

                Debe tener exactamente 5 alternativas, con una unica respuesta correcta. \
                Incluye una explicacion breve de por que esa alternativa es la correcta.
                """.formatted(temaNombre, dificultad.name());

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

    // Segunda llamada de validacion: le mostramos el enunciado y las alternativas
    // (SIN decirle cual es la clave) y le pedimos que resuelva la pregunta por su
    // cuenta. Si el indice que elige no coincide con claveCorrecta, la pregunta
    // se descarta (ver GeminiPreguntaValidator) en vez de guardarse a ciegas.
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

    private JsonNode llamar(String prompt, ObjectNode responseSchema) {
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode content = body.putArray("contents").addObject();
        content.putArray("parts").addObject().put("text", prompt);

        ObjectNode generationConfig = body.putObject("generationConfig");
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.set("responseSchema", responseSchema);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new GeminiException("Gemini respondio " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String textoJson = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            return objectMapper.readTree(textoJson);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new GeminiException("Error llamando a la API de Gemini", ex);
        }
    }
}
