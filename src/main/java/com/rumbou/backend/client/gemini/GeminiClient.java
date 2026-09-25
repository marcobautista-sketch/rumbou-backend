package com.rumbou.backend.client.gemini;

import com.rumbou.backend.entity.Dificultad;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    // 429, 503 y timeout son transitorios. La cuota se mide por minuto, asi que
    // la espera debe ser larga (30/60/90 s) o el reintento cae en el mismo minuto.
    private static final int MAX_INTENTOS_TRANSITORIO = 4;
    private static final Duration ESPERA_BASE_TRANSITORIO = Duration.ofSeconds(30);

    // Un JSON que no calza con el DTO es raro con responseSchema; se reintenta una vez.
    private static final int MAX_INTENTOS_JSON_LOTE = 2;

    private final String apiKey;
    private final String url;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Configurable (GEMINI_MODEL): Google retira modelos y no todas las keys ven los mismos.
    public GeminiClient(@Value("${gemini.api-key:}") String apiKey,
                        @Value("${gemini.model:gemini-3.6-flash}") String model,
                        ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.url = API_BASE + model + ":generateContent";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = objectMapper;
    }

    // Las "cantidad" preguntas del par en una sola llamada; minItems/maxItems fijan el tamano.
    public List<PreguntaGeneradaDto> generarPreguntas(String temaNombre, String temario, Dificultad dificultad,
                                                        int cantidad) {
        String temarioTexto = (temario == null || temario.isBlank())
                ? ""
                : "\n\nLimita las preguntas al siguiente temario oficial del tema "
                        + "(no salgas de estos subtemas):\n" + temario + "\n";

        String prompt = """
                Genera %d preguntas de opcion multiple, estilo examen de admision universitaria \
                peruano (UNI/UNMSM), sobre el tema "%s", con dificultad %s.%s

                No copies textualmente preguntas de examenes oficiales reales: usalas solo como \
                referencia de estilo y nivel de dificultad. Cada pregunta debe ser original, y \
                entre ellas deben cubrir subtemas distintos (no repitas el mismo subtema dos veces \
                si el temario lo permite).

                Cada pregunta debe tener exactamente 5 alternativas, con una unica respuesta \
                correcta. Incluye una explicacion breve de por que esa alternativa es la correcta.
                """.formatted(cantidad, temaNombre, dificultad.name(), temarioTexto);

        ObjectNode itemSchema = objectMapper.createObjectNode();
        itemSchema.put("type", "OBJECT");
        ObjectNode properties = itemSchema.putObject("properties");
        properties.putObject("enunciado").put("type", "STRING");
        ObjectNode alternativas = properties.putObject("alternativas");
        alternativas.put("type", "ARRAY");
        alternativas.putObject("items").put("type", "STRING");
        properties.putObject("claveCorrecta").put("type", "INTEGER");
        properties.putObject("explicacion").put("type", "STRING");
        itemSchema.putArray("required").add("enunciado").add("alternativas").add("claveCorrecta").add("explicacion");

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "ARRAY");
        schema.put("minItems", cantidad);
        schema.put("maxItems", cantidad);
        schema.set("items", itemSchema);

        int intento = 1;
        while (true) {
            JsonNode respuesta = llamar(prompt, schema);
            try {
                return objectMapper.convertValue(respuesta, new TypeReference<List<PreguntaGeneradaDto>>() {
                });
            } catch (IllegalArgumentException ex) {
                GeminiException error = new GeminiException(
                        "Gemini devolvio un lote de preguntas con formato invalido para "
                                + temaNombre + " / " + dificultad, ex);
                if (intento >= MAX_INTENTOS_JSON_LOTE) {
                    throw error;
                }
                log.warn("Intento {} de {}: {}", intento, MAX_INTENTOS_JSON_LOTE, error.getMessage());
                intento++;
            }
        }
    }

    // Una llamada por tema con las tres dificultades a la vez (la cuota diaria
    // gratuita no alcanza para una por dificultad). Devuelve agrupado por dificultad.
    public Map<Dificultad, List<PreguntaGeneradaDto>> generarPreguntasPorTema(String temaNombre, String temario,
                                                                              Map<Dificultad, Integer> cantidades) {
        int total = cantidades.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return Map.of();
        }

        String temarioTexto = (temario == null || temario.isBlank())
                ? ""
                : "\n\nLimita las preguntas al siguiente temario oficial del tema "
                        + "(no salgas de estos subtemas):\n" + temario + "\n";

        StringBuilder reparto = new StringBuilder();
        for (Dificultad dificultad : Dificultad.values()) {
            int n = cantidades.getOrDefault(dificultad, 0);
            if (n > 0) {
                reparto.append("- ").append(n).append(" con dificultad ").append(dificultad.name()).append("\n");
            }
        }

        String prompt = """
                Genera %d preguntas de opcion multiple, estilo examen de admision universitaria \
                peruano (UNI/UNMSM), sobre el tema "%s", repartidas asi:
                %s
                Cada pregunta debe indicar en el campo "dificultad" a cual de esos grupos pertenece \
                (FACIL, MEDIA o DIFICIL), respetando exactamente las cantidades pedidas.%s

                No copies textualmente preguntas de examenes oficiales reales: usalas solo como \
                referencia de estilo y nivel de dificultad. Cada pregunta debe ser original, y \
                entre ellas deben cubrir subtemas distintos (no repitas el mismo subtema dos veces \
                si el temario lo permite).

                Cada pregunta debe tener exactamente 5 alternativas, con una unica respuesta \
                correcta. Incluye una explicacion breve de por que esa alternativa es la correcta.
                """.formatted(total, temaNombre, reparto, temarioTexto);

        ObjectNode itemSchema = objectMapper.createObjectNode();
        itemSchema.put("type", "OBJECT");
        ObjectNode properties = itemSchema.putObject("properties");
        ObjectNode dificultadProp = properties.putObject("dificultad");
        dificultadProp.put("type", "STRING");
        dificultadProp.putArray("enum").add("FACIL").add("MEDIA").add("DIFICIL");
        properties.putObject("enunciado").put("type", "STRING");
        ObjectNode alternativas = properties.putObject("alternativas");
        alternativas.put("type", "ARRAY");
        alternativas.putObject("items").put("type", "STRING");
        properties.putObject("claveCorrecta").put("type", "INTEGER");
        properties.putObject("explicacion").put("type", "STRING");
        itemSchema.putArray("required").add("dificultad").add("enunciado").add("alternativas")
                .add("claveCorrecta").add("explicacion");

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "ARRAY");
        schema.put("minItems", total);
        schema.put("maxItems", total);
        schema.set("items", itemSchema);

        int intento = 1;
        while (true) {
            JsonNode respuesta = llamar(prompt, schema);
            try {
                List<PreguntaGeneradaConDificultadDto> generadas = objectMapper.convertValue(
                        respuesta, new TypeReference<List<PreguntaGeneradaConDificultadDto>>() {
                        });
                Map<Dificultad, List<PreguntaGeneradaDto>> porDificultad = new EnumMap<>(Dificultad.class);
                for (PreguntaGeneradaConDificultadDto generada : generadas) {
                    porDificultad.computeIfAbsent(generada.dificultad(), d -> new ArrayList<>())
                            .add(generada.sinDificultad());
                }
                return porDificultad;
            } catch (IllegalArgumentException ex) {
                GeminiException error = new GeminiException(
                        "Gemini devolvio un lote de preguntas con formato invalido para " + temaNombre, ex);
                if (intento >= MAX_INTENTOS_JSON_LOTE) {
                    throw error;
                }
                log.warn("Intento {} de {}: {}", intento, MAX_INTENTOS_JSON_LOTE, error.getMessage());
                intento++;
            }
        }
    }

    // Validacion: no se le da la clave; la respuesta se compara en GeminiPreguntaValidator.
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

    // resolver() en lote: una llamada para las N preguntas; clave por pregunta en orden (-1 si falta).
    public List<Integer> resolverLote(List<PreguntaGeneradaDto> preguntas) {
        JsonNode respuesta = llamar(promptResolverLote(preguntas), esquemaResolverLote(preguntas.size()));
        List<Integer> claves = new ArrayList<>();
        for (int p = 0; p < preguntas.size(); p++) {
            claves.add(respuesta.path(p).path("claveElegida").asInt(-1));
        }
        return claves;
    }

    private String promptResolverLote(List<PreguntaGeneradaDto> preguntas) {
        StringBuilder prompt = new StringBuilder("Resuelve cada una de las siguientes preguntas de opcion multiple. ");
        prompt.append("Para cada pregunta responde unicamente con el indice (0 a 4) de la alternativa correcta, ");
        prompt.append("en el mismo orden en que aparecen.\n\n");
        for (int p = 0; p < preguntas.size(); p++) {
            PreguntaGeneradaDto pregunta = preguntas.get(p);
            prompt.append("Pregunta ").append(p + 1).append(":\n").append(pregunta.enunciado()).append("\n");
            for (int i = 0; i < pregunta.alternativas().size(); i++) {
                prompt.append(i).append(") ").append(pregunta.alternativas().get(i)).append("\n");
            }
            prompt.append("\n");
        }
        return prompt.toString();
    }

    private ObjectNode esquemaResolverLote(int cantidad) {
        ObjectNode itemSchema = objectMapper.createObjectNode();
        itemSchema.put("type", "OBJECT");
        itemSchema.putObject("properties").putObject("claveElegida").put("type", "INTEGER");
        itemSchema.putArray("required").add("claveElegida");

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "ARRAY");
        schema.put("minItems", cantidad);
        schema.put("maxItems", cantidad);
        schema.set("items", itemSchema);
        return schema;
    }

    // A diferencia de resolver(), aqui si le damos la clave: el objetivo es explicar, no validar.
    public String explicar(String enunciado, List<String> alternativas, int claveCorrecta) {
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
                    .uri(URI.create(url + "?key=" + apiKey))
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

    // 429, 503 y timeout se reintentan con espera creciente; cualquier otro
    // fallo es definitivo.
    private HttpResponse<String> enviarConReintento(HttpRequest request) {
        for (int intento = 1; ; intento++) {
            Optional<HttpResponse<String>> respuesta = enviarUnaVez(request, intento);
            if (respuesta.isEmpty()) {
                esperarAntesDeReintentar(intento, Optional.empty());
                continue;
            }
            HttpResponse<String> response = respuesta.get();
            if (response.statusCode() == 200) {
                return response;
            }
            verificarReintentable(response, intento);
            Optional<Duration> retryAfter = response.headers().firstValue("Retry-After")
                    .flatMap(GeminiClient::parsearSegundos);
            log.warn("Gemini respondio {} (intento {} de {}), se reintenta en {} s",
                    response.statusCode(), intento, MAX_INTENTOS_TRANSITORIO,
                    retryAfter.orElse(ESPERA_BASE_TRANSITORIO.multipliedBy(intento)).toSeconds());
            esperarAntesDeReintentar(intento, retryAfter);
        }
    }

    // Vacio significa timeout con intentos disponibles: el llamador espera y reintenta.
    private Optional<HttpResponse<String>> enviarUnaVez(HttpRequest request, int intento) {
        try {
            return Optional.of(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
        } catch (HttpTimeoutException ex) {
            if (intento >= MAX_INTENTOS_TRANSITORIO) {
                throw new GeminiException("Gemini no respondio a tiempo despues de "
                        + MAX_INTENTOS_TRANSITORIO + " intentos", ex);
            }
            log.warn("Timeout llamando a Gemini (intento {} de {}), se reintenta", intento, MAX_INTENTOS_TRANSITORIO);
            return Optional.empty();
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new GeminiException("Error llamando a la API de Gemini", ex);
        }
    }

    private void verificarReintentable(HttpResponse<String> response, int intento) {
        if (!esTransitorio(response.statusCode())) {
            throw new GeminiException("Gemini respondio " + response.statusCode() + ": " + response.body());
        }
        if (intento >= MAX_INTENTOS_TRANSITORIO) {
            throw new GeminiException("Gemini sigue respondiendo " + response.statusCode()
                    + " despues de " + MAX_INTENTOS_TRANSITORIO + " intentos");
        }
    }

    private boolean esTransitorio(int statusCode) {
        return statusCode == 429 || statusCode == 503;
    }

    private static Optional<Duration> parsearSegundos(String valor) {
        try {
            return Optional.of(Duration.ofSeconds(Long.parseLong(valor.trim())));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    // Retry-After manda si vino; si no, 30 s, 60 s, 90 s.
    private void esperarAntesDeReintentar(int intento, Optional<Duration> retryAfter) {
        Duration espera = retryAfter.orElse(ESPERA_BASE_TRANSITORIO.multipliedBy(intento));
        try {
            Thread.sleep(espera.toMillis());
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
