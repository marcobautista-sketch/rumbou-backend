package com.rumbou.backend.repository;

import com.rumbou.backend.entity.EstadoSimulacro;
import com.rumbou.backend.entity.Simulacro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SimulacroRepository extends JpaRepository<Simulacro, Long> {

    // Progreso: el PSP mas reciente del usuario en un area.
    Optional<Simulacro> findFirstByUsuarioIdAndAreaIdAndEstadoOrderByFechaFinDesc(
            Long usuarioId, Long areaId, EstadoSimulacro estado);

    // Progreso: el historico de PSP del usuario en un area, del mas antiguo al mas reciente.
    List<Simulacro> findByUsuarioIdAndAreaIdAndEstadoOrderByFechaFinAsc(
            Long usuarioId, Long areaId, EstadoSimulacro estado);
}
