package com.rumbou.backend.client.youtube;

public record VideoYouTubeDto(
        String videoId,
        String titulo,
        String canal,
        String miniaturaUrl
) {
}
