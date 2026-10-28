package com.rumbou.backend.client.googlebooks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

// Google Books API v1. Sin clave responde 429 casi siempre, por eso la consulta sale del servidor.
@Component
public class GoogleBooksClient {

    private static final String VOLUMES_URL = "https://www.googleapis.com/books/v1/volumes";

    // Google Books responde 503 a ratos aunque la clave este bien: dos reintentos rapidos bastan casi siempre.
    private static final int MAX_INTENTOS = 3;
    private static final Duration PAUSA_ENTRE_INTENTOS = Duration.ofMillis(300);

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Por defecto usa la misma clave de Google Cloud que YouTube (las dos APIs van en un solo proyecto).
    public GoogleBooksClient(@Value("${google-books.api-key:}") String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
    }

    public boolean estaConfigurado() {
        return !apiKey.isBlank();
    }

    public List<LibroGoogleDto> buscarLibros(String consulta, int cantidad) {
        String url = VOLUMES_URL + "?langRestrict=es&printType=books"
                + "&maxResults=" + cantidad
                + "&q=" + URLEncoder.encode("intitle:" + consulta, StandardCharsets.UTF_8)
                + "&key=" + apiKey;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        for (int intento = 1; ; intento++) {
            HttpResponse<String> response = enviar(request);
            if (response.statusCode() == 200) {
                return parsearLibros(response.body());
            }
            boolean transitorio = response.statusCode() == 503 || response.statusCode() == 429;
            if (!transitorio || intento >= MAX_INTENTOS) {
                throw new GoogleBooksException("Google Books respondio " + response.statusCode());
            }
            esperar();
        }
    }

    // Separado de la llamada HTTP para probarlo sin red.
    List<LibroGoogleDto> parsearLibros(String json) {
        JsonNode items;
        try {
            items = objectMapper.readTree(json).path("items");
        } catch (IOException ex) {
            throw new GoogleBooksException("Google Books devolvio una respuesta que no se pudo interpretar", ex);
        }
        List<LibroGoogleDto> libros = new ArrayList<>();
        for (JsonNode item : items) {
            JsonNode volumen = item.path("volumeInfo");
            String titulo = volumen.path("title").asText("");
            if (titulo.isBlank()) {
                continue;
            }
            List<String> autores = new ArrayList<>();
            volumen.path("authors").forEach(autor -> autores.add(autor.asText()));
            String fecha = volumen.path("publishedDate").asText("");
            String portada = volumen.path("imageLinks").path("thumbnail").asText(null);
            libros.add(new LibroGoogleDto(
                    titulo,
                    autores,
                    fecha.length() >= 4 ? fecha.substring(0, 4) : null,
                    // Las portadas llegan por http: el navegador las bloquearia en una pagina https.
                    portada == null ? null : portada.replace("http://", "https://"),
                    volumen.path("infoLink").asText(null)
            ));
        }
        return libros;
    }

    private HttpResponse<String> enviar(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new GoogleBooksException("No se pudo conectar con Google Books", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GoogleBooksException("Se interrumpio la busqueda en Google Books", ex);
        }
    }

    private void esperar() {
        try {
            Thread.sleep(PAUSA_ENTRE_INTENTOS.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GoogleBooksException("Se interrumpio la busqueda en Google Books", ex);
        }
    }
}
