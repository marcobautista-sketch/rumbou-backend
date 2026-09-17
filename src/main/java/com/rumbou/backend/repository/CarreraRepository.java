package com.rumbou.backend.repository;

import com.rumbou.backend.entity.Carrera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarreraRepository extends JpaRepository<Carrera, Long> {

    Optional<Carrera> findByNombre(String nombre);
}
