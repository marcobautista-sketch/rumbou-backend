package com.rumbou.backend.repository;

import com.rumbou.backend.entity.RespuestaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RespuestaUsuarioRepository extends JpaRepository<RespuestaUsuario, Long> {

    List<RespuestaUsuario> findBySimulacroId(Long simulacroId);

    Optional<RespuestaUsuario> findBySimulacroIdAndPreguntaId(Long simulacroId, Long preguntaId);
}
