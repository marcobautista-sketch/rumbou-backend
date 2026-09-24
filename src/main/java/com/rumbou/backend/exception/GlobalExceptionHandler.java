package com.rumbou.backend.exception;

import com.rumbou.backend.dto.response.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Todas las excepciones propias: el codigo HTTP lo trae la excepcion.
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        return buildResponse(ex.getStatus(), ex.getMessage(), request);
    }

    // 502: el error viene de un tercero. No se expone ex.getMessage(): puede traer la respuesta cruda del proveedor.
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalService(ExternalServiceException ex,
                                                                  HttpServletRequest request) {
        log.warn("Fallo de servicio externo en {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_GATEWAY, "El servicio externo no respondio correctamente", request);
    }

    // BadCredentialsException, DisabledException, etc.: nunca se dice si el email existe.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex,
                                                                 HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Credenciales invalidas", request);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwt(JwtException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "El token es invalido o expiro", request);
    }

    // Lo que lanza @PreAuthorize cuando el rol no alcanza.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                               HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta accion", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    // Validaciones de Bean Validation fuera del cuerpo (parametros o entidades al persistir).
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                      HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex,
                                                                HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON valido", request);
    }

    // Parametro de URL ausente (?areaId=) o con un tipo que no convierte (?areaId=abc).
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleParametroInvalido(Exception ex,
                                                                    HttpServletRequest request) {
        String message = ex instanceof MissingServletRequestParameterException missing
                ? "Falta el parametro requerido: " + missing.getParameterName()
                : "El parametro " + ((MethodArgumentTypeMismatchException) ex).getName() + " tiene un valor invalido";
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex,
                                                                HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Falta el header requerido: " + ex.getHeaderName(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                     HttpServletRequest request) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED,
                "El metodo " + ex.getMethod() + " no esta permitido en este recurso", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                        HttpServletRequest request) {
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El cuerpo debe enviarse como application/json", request);
    }

    // Un unique o una llave foranea de la BD rechazo la operacion (p. ej. dos requests simultaneos).
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                HttpServletRequest request) {
        log.warn("Violacion de integridad en {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return buildResponse(HttpStatus.CONFLICT, "La operacion entra en conflicto con datos existentes", request);
    }

    // @Version: otro request modifico el mismo registro al mismo tiempo.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                                                 HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "El recurso fue modificado por otra operacion; intenta de nuevo", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex,
                                                                  HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Recurso no encontrado", request);
    }

    // Respaldo: el detalle va al log, nunca al cliente (podria traer SQL o rutas internas).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex,
                                                                   HttpServletRequest request) {
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error interno inesperado", request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
