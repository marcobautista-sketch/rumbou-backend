package com.rumbou.backend.repository;

import com.rumbou.backend.entity.ObjetivoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ObjetivoUsuarioRepository extends JpaRepository<ObjetivoUsuario, Long> {

    List<ObjetivoUsuario> findByUsuarioIdAndActivoTrue(Long usuarioId);

    long countByUsuarioIdAndActivoTrue(Long usuarioId);

    Optional<ObjetivoUsuario> findByUsuarioIdAndOfertaAcademicaId(Long usuarioId, Long ofertaAcademicaId);

    // Los objetivos que se actualizan al finalizar un simulacro de esa area.
    List<ObjetivoUsuario> findByUsuarioIdAndActivoTrueAndOfertaAcademicaAreaId(Long usuarioId, Long areaId);
}
