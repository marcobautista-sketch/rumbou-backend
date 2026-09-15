package com.rumbou.backend.suscripcion;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoWebhookRepository extends JpaRepository<PagoWebhook, Long> {

    boolean existsByPaymentId(Long paymentId);
}