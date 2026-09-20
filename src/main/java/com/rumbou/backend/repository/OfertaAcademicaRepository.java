package com.rumbou.backend.repository;

import com.rumbou.backend.entity.OfertaAcademica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OfertaAcademicaRepository extends JpaRepository<OfertaAcademica, Long> {

    Optional<OfertaAcademica> findByUniversidadIdAndCarreraIdAndAreaIdAndProcesoAdmision(
            Long universidadId, Long carreraId, Long areaId, String procesoAdmision);

    // Catalogo para elegir objetivo. Cada filtro se ignora si llega en null;
    // la carrera se busca por coincidencia parcial y sin distinguir mayusculas.
    // El CAST es obligatorio: con :carrera en null, PostgreSQL no puede deducir el
    // tipo del parametro dentro del CONCAT y falla con "function lower(bytea) does not exist".
    @Query("""
            SELECT o FROM OfertaAcademica o
            WHERE (:siglas IS NULL OR o.universidad.siglas = :siglas)
              AND (:codigoArea IS NULL OR o.area.codigo = :codigoArea)
              AND (:carrera IS NULL OR LOWER(o.carrera.nombre) LIKE LOWER(CONCAT('%', CAST(:carrera AS string), '%')))
            ORDER BY o.universidad.siglas, o.carrera.nombre
            """)
    List<OfertaAcademica> buscar(@Param("siglas") String siglas,
                                 @Param("codigoArea") String codigoArea,
                                 @Param("carrera") String carrera);
}
