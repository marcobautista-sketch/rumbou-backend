package com.rumbou.backend.client.googlebooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Solo la lectura de la respuesta: la llamada real a Google Books no se hace en los tests.
class GoogleBooksClientTest {

    private final GoogleBooksClient client = new GoogleBooksClient("clave-de-prueba", new ObjectMapper());

    @Test
    void leeTituloAutoresAnioYPortadaPorHttps() {
        String json = """
                {"items": [{"volumeInfo": {
                  "title": "Aritmética",
                  "authors": ["Aurelio Baldor"],
                  "publishedDate": "1983-05-01",
                  "infoLink": "https://books.google.com/books?id=abc",
                  "imageLinks": {"thumbnail": "http://books.google.com/books/content?id=abc&img=1"}
                }}]}
                """;

        List<LibroGoogleDto> libros = client.parsearLibros(json);

        assertThat(libros).containsExactly(new LibroGoogleDto("Aritmética", List.of("Aurelio Baldor"), "1983",
                "https://books.google.com/books/content?id=abc&img=1", "https://books.google.com/books?id=abc"));
    }

    @Test
    void toleraLibrosSinAutoresFechaNiPortadaYDescartaLosSinTitulo() {
        String json = """
                {"items": [
                  {"volumeInfo": {"title": "Química general", "infoLink": "https://books.google.com/b"}},
                  {"volumeInfo": {"authors": ["Sin titulo"]}}
                ]}
                """;

        List<LibroGoogleDto> libros = client.parsearLibros(json);

        assertThat(libros).hasSize(1);
        assertThat(libros.get(0).autores()).isEmpty();
        assertThat(libros.get(0).anio()).isNull();
        assertThat(libros.get(0).portadaUrl()).isNull();
    }

    @Test
    void sinResultadosDevuelveUnaListaVacia() {
        assertThat(client.parsearLibros("{\"totalItems\": 0}")).isEmpty();
    }

    @Test
    void unaRespuestaQueNoEsJsonLanzaGoogleBooksException() {
        assertThatThrownBy(() -> client.parsearLibros("Service Unavailable"))
                .isInstanceOf(GoogleBooksException.class);
    }
}
