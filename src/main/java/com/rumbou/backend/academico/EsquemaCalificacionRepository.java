package com.rumbou.backend.academico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EsquemaCalificacionRepository extends JpaRepository<EsquemaCalificacion, Long> {

    List<EsquemaCalificacion> findByUniversidadIdOrderByOrden(Long universidadId);

    Optional<EsquemaCalificacion> findByUniversidadIdAndNombreBloque(Long universidadId, String nombreBloque);
}
