package com.rumbou.backend.academico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstructuraExamenRepository extends JpaRepository<EstructuraExamen, Long> {

    List<EstructuraExamen> findByAreaIdOrderByOrden(Long areaId);

    Optional<EstructuraExamen> findByAreaIdAndTemaId(Long areaId, Long temaId);
}
