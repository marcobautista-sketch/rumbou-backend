package com.rumbou.backend.academico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstructuraExamenRepository extends JpaRepository<EstructuraExamen, Long> {

    List<EstructuraExamen> findByAreaIdOrderByOrden(Long areaId);
}
