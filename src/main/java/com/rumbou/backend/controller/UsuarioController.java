package com.rumbou.backend.controller;

import com.rumbou.backend.dto.request.CambiarRolRequest;
import com.rumbou.backend.dto.response.GamificacionResponse;
import com.rumbou.backend.dto.response.UsuarioResponse;
import com.rumbou.backend.service.GamificacionService;
import com.rumbou.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final GamificacionService gamificacionService;

    public UsuarioController(UsuarioService usuarioService, GamificacionService gamificacionService) {
        this.usuarioService = usuarioService;
        this.gamificacionService = gamificacionService;
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me() {
        return ResponseEntity.ok(usuarioService.obtenerPerfil());
    }

    @GetMapping("/me/gamificacion")
    public ResponseEntity<GamificacionResponse> gamificacion() {
        return ResponseEntity.ok(gamificacionService.obtenerResumen());
    }

    // Para que un admin ubique el id de la cuenta que quiere promover.
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> buscarPorEmail(@RequestParam String email) {
        return ResponseEntity.ok(usuarioService.buscarPorEmail(email));
    }

    @PatchMapping("/{id}/rol")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> cambiarRol(@PathVariable Long id,
                                                      @Valid @RequestBody CambiarRolRequest request) {
        return ResponseEntity.ok(usuarioService.cambiarRol(id, request.role()));
    }
}
