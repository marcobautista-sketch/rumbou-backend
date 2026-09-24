package com.rumbou.backend.repository;

import com.rumbou.backend.entity.UsuarioLogro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsuarioLogroRepository extends JpaRepository<UsuarioLogro, Long> {

    boolean existsByUsuarioIdAndLogroId(Long usuarioId, Long logroId);

    // JOIN FETCH: el logro es LAZY y la respuesta necesita su nombre y descripcion.
    @Query("""
            SELECT ul FROM UsuarioLogro ul JOIN FETCH ul.logro
            WHERE ul.usuario.id = :usuarioId
            ORDER BY ul.fechaDesbloqueo
            """)
    List<UsuarioLogro> findByUsuarioIdConLogro(@Param("usuarioId") Long usuarioId);
}
