package com.rumbou.backend.service.impl;

import com.rumbou.backend.client.youtube.VideoYouTubeDto;
import com.rumbou.backend.client.youtube.YouTubeClient;
import com.rumbou.backend.client.youtube.YouTubeException;
import com.rumbou.backend.dto.response.VideoResponse;
import com.rumbou.backend.dto.response.VideosTemaResponse;
import com.rumbou.backend.entity.Tema;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.repository.TemaRepository;
import com.rumbou.backend.service.VideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VideoServiceImpl implements VideoService {

    private static final Logger log = LoggerFactory.getLogger(VideoServiceImpl.class);

    private static final int VIDEOS_POR_TEMA = 3;

    // Cada busqueda gasta 100 de las 10 000 unidades diarias gratuitas: con un dia de cache,
    // los ~20 temas del banco caben de sobra en la cuota.
    private static final Duration DURACION_CACHE = Duration.ofHours(24);

    private final TemaRepository temaRepository;
    private final YouTubeClient youTubeClient;
    private final Map<Long, VideosEnCache> cache = new ConcurrentHashMap<>();

    private record VideosEnCache(List<VideoResponse> videos, Instant guardadoEn) {
    }

    public VideoServiceImpl(TemaRepository temaRepository, YouTubeClient youTubeClient) {
        this.temaRepository = temaRepository;
        this.youTubeClient = youTubeClient;
    }

    @Override
    public VideosTemaResponse videosDelTema(Long temaId) {
        Tema tema = temaRepository.findById(temaId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un tema con id " + temaId));
        // "preuniversitario" trae clases de academias peruanas en vez de cursos universitarios.
        String consulta = tema.getNombre() + " preuniversitario";
        String busquedaUrl = "https://www.youtube.com/results?search_query="
                + URLEncoder.encode(consulta, StandardCharsets.UTF_8);
        return new VideosTemaResponse(tema.getId(), tema.getNombre(), buscarConCache(temaId, consulta), busquedaUrl);
    }

    // Sin clave, sin cuota o con YouTube caido, el plan de estudio no falla: queda el enlace de busqueda.
    private List<VideoResponse> buscarConCache(Long temaId, String consulta) {
        VideosEnCache enCache = cache.get(temaId);
        if (enCache != null && enCache.guardadoEn().plus(DURACION_CACHE).isAfter(Instant.now())) {
            return enCache.videos();
        }
        if (!youTubeClient.estaConfigurado()) {
            return List.of();
        }
        try {
            List<VideoResponse> videos = youTubeClient.buscarVideos(consulta, VIDEOS_POR_TEMA).stream()
                    .map(VideoServiceImpl::toResponse)
                    .toList();
            cache.put(temaId, new VideosEnCache(videos, Instant.now()));
            return videos;
        } catch (YouTubeException ex) {
            log.warn("No se pudieron traer videos de YouTube para el tema {}: {}", temaId, ex.getMessage());
            return List.of();
        }
    }

    private static VideoResponse toResponse(VideoYouTubeDto video) {
        return new VideoResponse(video.videoId(), video.titulo(), video.canal(), video.miniaturaUrl(),
                "https://www.youtube.com/watch?v=" + video.videoId());
    }
}
