package com.rumbou.backend.repository;

import com.rumbou.backend.entity.UsuarioLogro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioLogroRepository extends JpaRepository<UsuarioLogro, Long> {

    boolean existsByUsuarioIdAndLogroId(Long usuarioId, Long logroId);
}