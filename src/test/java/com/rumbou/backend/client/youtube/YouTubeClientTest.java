package com.rumbou.backend.client.youtube;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Solo la lectura de la respuesta: la llamada real a YouTube no se hace en los tests.
class YouTubeClientTest {

    private final YouTubeClient client = new YouTubeClient("clave-de-prueba", new ObjectMapper());

    @Test
    void leeIdTituloCanalYMiniaturaDecodificandoLasEntidadesHtml() {
        String json = """
                {"items": [{
                  "id": {"kind": "youtube#video", "videoId": "abc123"},
                  "snippet": {
                    "title": "Aritm\\u00e9tica: &quot;MCD y MCM&quot; | Repaso &amp; ejercicios",
                    "channelTitle": "Academia &#39;Pre&#39;",
                    "thumbnails": {"medium": {"url": "https://i.ytimg.com/vi/abc123/mqdefault.jpg"}}
                  }
                }]}
                """;

        List<VideoYouTubeDto> videos = client.parsearVideos(json);

        assertThat(videos).containsExactly(new VideoYouTubeDto("abc123",
                "Aritmética: \"MCD y MCM\" | Repaso & ejercicios", "Academia 'Pre'",
                "https://i.ytimg.com/vi/abc123/mqdefault.jpg"));
    }

    @Test
    void ignoraResultadosQueNoSonVideos() {
        String json = """
                {"items": [
                  {"id": {"kind": "youtube#channel", "channelId": "UC1"}, "snippet": {"title": "Un canal"}},
                  {"id": {"kind": "youtube#video", "videoId": "v1"}, "snippet": {"title": "Clase", "channelTitle": "Canal"}}
                ]}
                """;

        List<VideoYouTubeDto> videos = client.parsearVideos(json);

        assertThat(videos).extracting(VideoYouTubeDto::videoId).containsExactly("v1");
        assertThat(videos.get(0).miniaturaUrl()).isNull();
    }

    @Test
    void sinResultadosDevuelveUnaListaVacia() {
        assertThat(client.parsearVideos("{\"items\": []}")).isEmpty();
        assertThat(client.parsearVideos("{}")).isEmpty();
    }

    @Test
    void unaRespuestaQueNoEsJsonLanzaYouTubeException() {
        assertThatThrownBy(() -> client.parsearVideos("<html>error</html>"))
                .isInstanceOf(YouTubeException.class);
    }

    @Test
    void sinClaveNoEstaConfigurado() {
        assertThat(new YouTubeClient("", new ObjectMapper()).estaConfigurado()).isFalse();
        assertThat(client.estaConfigurado()).isTrue();
    }
}
