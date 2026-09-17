package com.rumbou.backend.repository;

import com.rumbou.backend.entity.PagoWebhook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoWebhookRepository extends JpaRepository<PagoWebhook, Long> {

    boolean existsByPaymentId(Long paymentId);
}