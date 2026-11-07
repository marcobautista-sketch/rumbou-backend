package com.rumbou.backend.repository;

import com.rumbou.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    // El email no distingue mayusculas: Ana@x.com y ana@x.com son la misma cuenta.
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("select count(u) > 0 from Usuario u where lower(u.aliasLiga) = lower(:alias) and u.id <> :usuarioId")
    boolean existeAliasLigaDeOtroUsuario(@Param("alias") String alias, @Param("usuarioId") Long usuarioId);

    // La reserva dura hasta el commit y tambien protege variantes de mayusculas entre servidores.
    // La comprobacion usa el mismo lower de PostgreSQL y se hace despues de adquirir la reserva.
    @Query(value = "select 1 from pg_advisory_xact_lock(hashtext(lower(cast(:alias as text))))", nativeQuery = true)
    Integer reservarAliasLiga(@Param("alias") String alias);
}
