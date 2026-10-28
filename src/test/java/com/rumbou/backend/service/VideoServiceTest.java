package com.rumbou.backend.service;

import com.rumbou.backend.client.youtube.VideoYouTubeDto;
import com.rumbou.backend.client.youtube.YouTubeClient;
import com.rumbou.backend.client.youtube.YouTubeException;
import com.rumbou.backend.dto.response.VideosTemaResponse;
import com.rumbou.backend.entity.AreaConocimiento;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.repository.TemaRepository;
import com.rumbou.backend.service.impl.VideoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class VideoServiceTest {

    @Mock
    private TemaRepository temaRepository;

    @Mock
    private YouTubeClient youTubeClient;

    private VideoServiceImpl videoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        videoService = new VideoServiceImpl(temaRepository, youTubeClient);
        Tema tema = new Tema("Historia del Perú", AreaConocimiento.HUMANIDADES);
        tema.setId(7L);
        given(temaRepository.findById(7L)).willReturn(Optional.of(tema));
        given(youTubeClient.estaConfigurado()).willReturn(true);
    }

    @Test
    void buscaClasesPreuniversitariasDelTemaYArmaElEnlaceDeCadaVideo() {
        given(youTubeClient.buscarVideos("Historia del Perú preuniversitario", 3))
                .willReturn(List.of(new VideoYouTubeDto("v1", "Repaso final", "Academia", "https://img/v1.jpg")));

        VideosTemaResponse respuesta = videoService.videosDelTema(7L);

        assertThat(respuesta.temaId()).isEqualTo(7L);
        assertThat(respuesta.tema()).isEqualTo("Historia del Perú");
        assertThat(respuesta.videos()).hasSize(1);
        assertThat(respuesta.videos().get(0).url()).isEqualTo("https://www.youtube.com/watch?v=v1");
        assertThat(respuesta.busquedaUrl())
                .isEqualTo("https://www.youtube.com/results?search_query=Historia+del+Per%C3%BA+preuniversitario");
    }

    @Test
    void laSegundaConsultaDelMismoTemaNoGastaOtraBusqueda() {
        given(youTubeClient.buscarVideos(anyString(), anyInt()))
                .willReturn(List.of(new VideoYouTubeDto("v1", "Repaso", "Academia", null)));

        videoService.videosDelTema(7L);
        VideosTemaResponse segunda = videoService.videosDelTema(7L);

        assertThat(segunda.videos()).hasSize(1);
        verify(youTubeClient, times(1)).buscarVideos(anyString(), anyInt());
    }

    @Test
    void sinClaveDevuelveSoloElEnlaceDeBusqueda() {
        given(youTubeClient.estaConfigurado()).willReturn(false);

        VideosTemaResponse respuesta = videoService.videosDelTema(7L);

        assertThat(respuesta.videos()).isEmpty();
        assertThat(respuesta.busquedaUrl()).startsWith("https://www.youtube.com/results?search_query=");
        verify(youTubeClient, never()).buscarVideos(anyString(), anyInt());
    }

    @Test
    void siYouTubeFallaElPlanSigueConElEnlaceYNoSeGuardaEnCache() {
        given(youTubeClient.buscarVideos(anyString(), anyInt()))
                .willThrow(new YouTubeException("YouTube respondio 403"))
                .willReturn(List.of(new VideoYouTubeDto("v1", "Repaso", "Academia", null)));

        assertThat(videoService.videosDelTema(7L).videos()).isEmpty();
        assertThat(videoService.videosDelTema(7L).videos()).hasSize(1);
    }

    @Test
    void unTemaInexistenteResponde404() {
        given(temaRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> videoService.videosDelTema(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
