package com.rumbou.backend.repository;

import com.rumbou.backend.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AreaRepository extends JpaRepository<Area, Long> {

    Optional<Area> findByUniversidadIdAndCodigo(Long universidadId, String codigo);
}
