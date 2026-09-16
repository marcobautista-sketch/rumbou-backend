package com.rumbou.backend.contenido;

import com.rumbou.backend.auth.Role;
import com.rumbou.backend.auth.Usuario;
import com.rumbou.backend.contenido.dto.AprobarLoteRequest;
import com.rumbou.backend.contenido.dto.CreatePreguntaRequest;
import com.rumbou.backend.contenido.dto.PreguntaAdminResponse;
import com.rumbou.backend.contenido.dto.PreguntaResponse;
import com.rumbou.backend.contenido.dto.TutorIaResponse;
import com.rumbou.backend.contenido.dto.UpdatePreguntaRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/preguntas")
public class PreguntaController {

    private static final int TAMANO_PAGINA_MAXIMO = 50;

    private final PreguntaService preguntaService;

    public PreguntaController(PreguntaService preguntaService) {
        this.preguntaService = preguntaService;
    }

    @GetMapping
    public ResponseEntity<Page<PreguntaResponse>> listar(@AuthenticationPrincipal Usuario usuario,
                                                            @RequestParam(required = false) Long temaId,
                                                            @RequestParam(required = false) Dificultad dificultad,
                                                            @RequestParam(required = false) OrigenPregunta origen,
                                                            @RequestParam(required = false) Boolean aprobada,
                                                            Pageable pageable) {
        Pageable pageableLimitado = limitarTamano(pageable);
        return ResponseEntity.ok(preguntaService.buscar(
                temaId, dificultad, origen, aprobada, pageableLimitado, esAdmin(usuario)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PreguntaResponse> obtener(@AuthenticationPrincipal Usuario usuario, @PathVariable Long id) {
        return ResponseEntity.ok(preguntaService.obtener(id, esAdmin(usuario)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PreguntaAdminResponse> crear(@Valid @RequestBody CreatePreguntaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(preguntaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PreguntaAdminResponse> actualizar(@PathVariable Long id,
                                                               @Valid @RequestBody UpdatePreguntaRequest request) {
        return ResponseEntity.ok(preguntaService.actualizar(id, request));
    }

    // Aprobar es una operacion mas chica y frecuente que un PUT completo: la usa
    // sobre todo el panel de revision de preguntas generadas con Gemini.
    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PreguntaAdminResponse> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(preguntaService.aprobar(id));
    }

    // Para el panel de revision: aprobar de un jalon todas las preguntas
    // generadas con Gemini que ya se revisaron para un tema y dificultad.
    @PatchMapping("/aprobar-lote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PreguntaAdminResponse>> aprobarLote(@Valid @RequestBody AprobarLoteRequest request) {
        return ResponseEntity.ok(preguntaService.aprobarLote(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        preguntaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // Sin @PreAuthorize: no es un tema de rol sino de plan, y eso lo decide
    // PlanService adentro del servicio, no el controller.
    @PostMapping("/{id}/tutor-ia")
    public ResponseEntity<TutorIaResponse> tutorIa(@AuthenticationPrincipal Usuario usuario, @PathVariable Long id) {
        return ResponseEntity.ok(preguntaService.pedirExplicacionTutorIa(id, usuario, esAdmin(usuario)));
    }

    private boolean esAdmin(Usuario usuario) {
        return usuario.getRole() == Role.ADMIN;
    }

    private Pageable limitarTamano(Pageable pageable) {
        if (pageable.getPageSize() <= TAMANO_PAGINA_MAXIMO) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), TAMANO_PAGINA_MAXIMO, pageable.getSort());
    }
}
