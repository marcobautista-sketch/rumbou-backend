package com.rumbou.backend.repository;

import com.rumbou.backend.entity.DesafioDiario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DesafioDiarioRepository extends JpaRepository<DesafioDiario, Long> {

    Optional<DesafioDiario> findByUsuarioIdAndFecha(Long usuarioId, LocalDate fecha);

    long countByUsuarioIdAndFechaBetweenAndFechaRespuestaIsNotNull(Long usuarioId, LocalDate desde, LocalDate hasta);

    @Query("SELECT d.pregunta.id FROM DesafioDiario d WHERE d.usuario.id = :usuarioId")
    List<Long> findPreguntaIdsByUsuarioId(@Param("usuarioId") Long usuarioId);
}
