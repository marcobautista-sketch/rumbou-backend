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
