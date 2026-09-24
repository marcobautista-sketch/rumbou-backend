package com.rumbou.backend.repository;

import com.rumbou.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    // El email no distingue mayusculas: Ana@x.com y ana@x.com son la misma cuenta.
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);
}
