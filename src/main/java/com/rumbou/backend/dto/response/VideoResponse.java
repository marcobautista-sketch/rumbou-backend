package com.rumbou.backend.dto.response;

public record VideoResponse(
        String videoId,
        String titulo,
        String canal,
        String miniaturaUrl,
        String url
) {
}
