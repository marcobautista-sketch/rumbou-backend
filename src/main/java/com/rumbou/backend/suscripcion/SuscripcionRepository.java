package com.rumbou.backend.suscripcion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {
    Optional<Suscripcion> findFirstByUsuarioIdAndEstadoOrderByFechaInicioDesc(
            Long usuarioId, EstadoSuscripcion estado);
}