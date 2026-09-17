package com.rumbou.backend.repository;

import com.rumbou.backend.entity.Universidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UniversidadRepository extends JpaRepository<Universidad, Long> {

    Optional<Universidad> findBySiglas(String siglas);
}
