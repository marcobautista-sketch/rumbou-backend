package com.rumbou.backend.gamificacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LogroRepository extends JpaRepository<Logro, Long> {

    List<Logro> findByCondicion(TipoLogro condicion);

    Optional<Logro> findByNombre(String nombre);
}