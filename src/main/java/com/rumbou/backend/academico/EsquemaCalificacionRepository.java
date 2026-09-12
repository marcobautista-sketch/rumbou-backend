package com.rumbou.backend.academico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EsquemaCalificacionRepository extends JpaRepository<EsquemaCalificacion, Long> {

    List<EsquemaCalificacion> findByUniversidadIdOrderByOrden(Long universidadId);
}
