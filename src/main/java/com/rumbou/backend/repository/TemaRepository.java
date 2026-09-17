package com.rumbou.backend.repository;

import com.rumbou.backend.entity.Tema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TemaRepository extends JpaRepository<Tema, Long> {

    Optional<Tema> findByNombre(String nombre);
}
