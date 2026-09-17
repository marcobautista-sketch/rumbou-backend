package com.rumbou.backend.repository;

import com.rumbou.backend.entity.Logro;
import com.rumbou.backend.entity.TipoLogro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LogroRepository extends JpaRepository<Logro, Long> {

    List<Logro> findByCondicion(TipoLogro condicion);

    Optional<Logro> findByNombre(String nombre);
}