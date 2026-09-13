package com.rumbou.backend.gamificacion;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioLogroRepository extends JpaRepository<UsuarioLogro, Long> {

    boolean existsByUsuarioIdAndLogroId(Long usuarioId, Long logroId);
}