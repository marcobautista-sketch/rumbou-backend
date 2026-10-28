package com.rumbou.backend.client.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

// YouTube Data API v3. La clave vive solo en el servidor: la web y el app nunca la ven.
@Component
public class YouTubeClient {

    private static final String SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public YouTubeClient(@Value("${youtube.api-key:}") String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
    }

    public boolean estaConfigurado() {
        return !apiKey.isBlank();
    }

    // Videos en espanol, priorizando Peru, y con busqueda segura (el publico son escolares).
    public List<VideoYouTubeDto> buscarVideos(String consulta, int cantidad) {
        String url = SEARCH_URL + "?part=snippet&type=video&safeSearch=strict&relevanceLanguage=es&regionCode=PE"
                + "&maxResults=" + cantidad
                + "&q=" + URLEncoder.encode(consulta, StandardCharsets.UTF_8)
                + "&key=" + apiKey;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new YouTubeException("No se pudo conectar con YouTube", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new YouTubeException("Se interrumpio la busqueda en YouTube", ex);
        }
        // 403 suele ser la cuota diaria agotada. No se copia el cuerpo: no aporta y es largo.
        if (response.statusCode() != 200) {
            throw new YouTubeException("YouTube respondio " + response.statusCode());
        }
        return parsearVideos(response.body());
    }

    // Separado de la llamada HTTP para probarlo sin red.
    List<VideoYouTubeDto> parsearVideos(String json) {
        JsonNode items;
        try {
            items = objectMapper.readTree(json).path("items");
        } catch (IOException ex) {
            throw new YouTubeException("YouTube devolvio una respuesta que no se pudo interpretar", ex);
        }
        List<VideoYouTubeDto> videos = new ArrayList<>();
        for (JsonNode item : items) {
            String videoId = item.path("id").path("videoId").asText("");
            if (videoId.isBlank()) {
                continue;
            }
            JsonNode snippet = item.path("snippet");
            // La API devuelve los titulos con entidades HTML (&quot;, &#39;, &amp;).
            videos.add(new VideoYouTubeDto(
                    videoId,
                    HtmlUtils.htmlUnescape(snippet.path("title").asText()),
                    HtmlUtils.htmlUnescape(snippet.path("channelTitle").asText()),
                    snippet.path("thumbnails").path("medium").path("url").asText(null)
            ));
        }
        return videos;
    }
}
