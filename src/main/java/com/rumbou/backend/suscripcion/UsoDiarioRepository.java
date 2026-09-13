package com.rumbou.backend.suscripcion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface UsoDiarioRepository extends JpaRepository<UsoDiario, Long> {

    Optional<UsoDiario> findByUsuarioIdAndFecha(Long usuarioId, LocalDate fecha);

    @Query("""
            select coalesce(sum(u.simulacrosTema), 0) from UsoDiario u
            where u.usuario.id = :usuarioId and u.fecha >= :desde
            """)
    int sumSimulacrosTemaDesde(@Param("usuarioId") Long usuarioId, @Param("desde") LocalDate desde);

    @Query("""
            select coalesce(sum(u.simulacrosCompletos), 0) from UsoDiario u
            where u.usuario.id = :usuarioId and u.fecha >= :desde
            """)
    int sumSimulacrosCompletosDesde(@Param("usuarioId") Long usuarioId, @Param("desde") LocalDate desde);

    @Query("""
            select coalesce(sum(u.consultasTutorIA), 0) from UsoDiario u
            where u.usuario.id = :usuarioId and u.fecha = :fecha
            """)
    int sumConsultasTutorIA(@Param("usuarioId") Long usuarioId, @Param("fecha") LocalDate fecha);
}