package com.rumbou.backend.service;

import com.rumbou.backend.dto.request.ParticipacionRankingRequest;
import com.rumbou.backend.dto.response.RankingResponse;

public interface RankingService {

    RankingResponse obtenerRanking(int page, int size);

    RankingResponse participar(ParticipacionRankingRequest request);

    void dejarDeParticipar();
}
