package com.rumbou.backend.repository;

import com.rumbou.backend.entity.EstadoSuscripcion;
import com.rumbou.backend.entity.Suscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    Optional<Suscripcion> findFirstByUsuarioIdAndEstadoOrderByFechaInicioDesc(
            Long usuarioId, EstadoSuscripcion estado);

    Optional<Suscripcion> findFirstByMercadoPagoPreapprovalId(String mercadoPagoPreapprovalId);

    List<Suscripcion> findByEstadoAndFechaFinBefore(EstadoSuscripcion estado, LocalDate fecha);
}