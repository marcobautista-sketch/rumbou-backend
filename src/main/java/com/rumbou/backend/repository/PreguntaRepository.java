package com.rumbou.backend.repository;

import com.rumbou.backend.entity.Dificultad;
import com.rumbou.backend.entity.OrigenPregunta;
import com.rumbou.backend.entity.Pregunta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {

    // Usado por SimulacroGeneratorService: no tocar la firma sin avisar a Marco.
    List<Pregunta> findByTemaIdAndAprobadaTrue(Long temaId);

    // Clave natural del seed de preguntas: (tema, enunciado).
    Optional<Pregunta> findByTemaIdAndEnunciado(Long temaId, String enunciado);

    // Usado por GeneradorPreguntasRunner en modo --todos para saltar un par
    // tema+dificultad que ya tiene suficientes preguntas (aprobadas o no).
    long countByTemaIdAndDificultad(Long temaId, Dificultad dificultad);

    // Usado por ExportadorPreguntasRunner para volcar el banco a preguntas.csv.
    // JOIN FETCH: el exportador ordena por tema.nombre, y tema es LAZY. Sin el
    // fetch, eso dispara un SELECT por pregunta en vez de uno solo.
    @Query("SELECT p FROM Pregunta p JOIN FETCH p.tema WHERE p.aprobada = true")
    List<Pregunta> findByAprobadaTrueConTema();

    // Cada parametro es opcional (null = no filtrar por ese campo).
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
