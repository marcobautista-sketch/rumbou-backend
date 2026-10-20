package com.rumbou.backend.service.impl;

import com.rumbou.backend.dto.response.AreaResponse;
import com.rumbou.backend.dto.response.TemaResponse;
import com.rumbou.backend.entity.Area;
import com.rumbou.backend.exception.ResourceNotFoundException;
import com.rumbou.backend.mapper.AreaMapper;
import com.rumbou.backend.repository.AreaRepository;
import com.rumbou.backend.repository.EstructuraExamenRepository;
import com.rumbou.backend.service.AreaService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AreaServiceImpl implements AreaService {

    private final AreaRepository areaRepository;
    private final EstructuraExamenRepository estructuraExamenRepository;

    public AreaServiceImpl(AreaRepository areaRepository, EstructuraExamenRepository estructuraExamenRepository) {
        this.areaRepository = areaRepository;
        this.estructuraExamenRepository = estructuraExamenRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<AreaResponse> listar() {
        return areaRepository.findAll(Sort.by("id")).stream()
                .map(AreaMapper::toResponse)
                .toList();
    }

    // Los temas de un area son los de su estructura de examen, en el orden del examen real.
    @Transactional(readOnly = true)
    @Override
    public List<TemaResponse> listarTemas(Long areaId) {
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un area con id " + areaId));
        String siglas = area.getUniversidad().getSiglas();
        return estructuraExamenRepository.findByAreaIdOrderByOrden(areaId).stream()
                .map(estructura -> AreaMapper.toTemaResponse(estructura, siglas))
                .toList();
    }
}
