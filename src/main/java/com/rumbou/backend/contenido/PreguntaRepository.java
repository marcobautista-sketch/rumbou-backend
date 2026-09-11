package com.rumbou.backend.contenido;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {

    // Usado por examen/SimulacroGeneratorService: no tocar la firma sin avisar a Marco.
    List<Pregunta> findByTemaIdAndAprobadaTrue(Long temaId);

    // Filtro combinado para el CRUD del panel de administracion. Cada parametro
    // es opcional (null = no filtrar por ese campo), asi evitamos escribir un
    // metodo derivado distinto por cada combinacion de filtros.
    @Query("""
            SELECT p FROM Pregunta p
            WHERE (:temaId IS NULL OR p.tema.id = :temaId)
            AND (:dificultad IS NULL OR p.dificultad = :dificultad)
            AND (:origen IS NULL OR p.origen = :origen)
            AND (:aprobada IS NULL OR p.aprobada = :aprobada)
            """)
    Page<Pregunta> buscar(@Param("temaId") Long temaId,
                           @Param("dificultad") Dificultad dificultad,
                           @Param("origen") OrigenPregunta origen,
                           @Param("aprobada") Boolean aprobada,
                           Pageable pageable);
}
