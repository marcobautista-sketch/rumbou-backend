package com.rumbou.backend.academico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfertaAcademicaRepository extends JpaRepository<OfertaAcademica, Long> {

    Optional<OfertaAcademica> findByUniversidadIdAndCarreraIdAndAreaIdAndProcesoAdmision(
            Long universidadId, Long carreraId, Long areaId, String procesoAdmision);
}
