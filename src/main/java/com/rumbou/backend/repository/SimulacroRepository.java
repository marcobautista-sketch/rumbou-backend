package com.rumbou.backend.repository;

import com.rumbou.backend.entity.EstadoSimulacro;
import com.rumbou.backend.entity.Simulacro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SimulacroRepository extends JpaRepository<Simulacro, Long> {

    // Historial del usuario; el area se trae en la misma consulta porque la respuesta muestra su codigo.
    @EntityGraph(attributePaths = "area")
    Page<Simulacro> findByUsuarioIdOrderByFechaInicioDesc(Long usuarioId, Pageable pageable);

    // Progreso: el PSP mas reciente del usuario en un area.
    Optional<Simulacro> findFirstByUsuarioIdAndAreaIdAndEstadoOrderByFechaFinDesc(
            Long usuarioId, Long areaId, EstadoSimulacro estado);

    // Progreso: el historico de PSP del usuario en un area, del mas antiguo al mas reciente.
    List<Simulacro> findByUsuarioIdAndAreaIdAndEstadoOrderByFechaFinAsc(
            Long usuarioId, Long areaId, EstadoSimulacro estado);
}
