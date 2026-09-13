package com.rumbou.backend.gamificacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogroRepository extends JpaRepository<Logro, Long> {

    List<Logro> findByCondicion(TipoLogro condicion);
}