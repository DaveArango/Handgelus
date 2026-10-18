package com.handgelus.infrastructure.adapter.in.web.soporte;

import com.handgelus.domain.exception.AccesoDenegadoException;
import com.handgelus.domain.exception.CredencialesInvalidasException;
import com.handgelus.domain.exception.DominioException;
import com.handgelus.domain.exception.RecursoNoEncontradoException;
import com.handgelus.domain.exception.ReglaNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones a respuestas JSON con mensajes en español claro (RNF-11).
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    public record RespuestaError(int estado, String mensaje, String regla, Map<String, String> campos,
                                 LocalDateTime fecha) {
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<RespuestaError> reglaNegocio(ReglaNegocioException e) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e.getRegla(), null);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaError> noEncontrado(RecursoNoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage(), null, null);
    }

    @ExceptionHandler({AccesoDenegadoException.class, AuthorizationDeniedException.class})
    public ResponseEntity<RespuestaError> accesoDenegado(RuntimeException e) {
        String mensaje = e instanceof AccesoDenegadoException ? e.getMessage()
                : "Su rol no tiene permiso para realizar esta acción.";
        return responder(HttpStatus.FORBIDDEN, mensaje, null, null);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<RespuestaError> credenciales(CredencialesInvalidasException e) {
        return responder(HttpStatus.UNAUTHORIZED, e.getMessage(), null, null);
    }

    @ExceptionHandler(DominioException.class)
    public ResponseEntity<RespuestaError> dominio(DominioException e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage(), null, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> validacion(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return responder(HttpStatus.BAD_REQUEST, "Revise los campos marcados en el formulario.", null, campos);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<RespuestaError> archivoGrande(MaxUploadSizeExceededException e) {
        return responder(HttpStatus.PAYLOAD_TOO_LARGE,
                "La imagen supera el máximo de 5 MB. Por favor adjunte una imagen de menor tamaño.", "RF-02", null);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<RespuestaError> peticionInvalida(Exception e) {
        return responder(HttpStatus.BAD_REQUEST, "La información enviada está incompleta o tiene un formato inválido.",
                null, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespuestaError> integridad(DataIntegrityViolationException e) {
        log.warn("Violación de integridad: {}", e.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "El registro entra en conflicto con información existente.", null, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<RespuestaError> rutaInexistente(NoResourceFoundException e) {
        return responder(HttpStatus.NOT_FOUND, "El recurso solicitado no existe.", null, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> inesperado(Exception e) {
        log.error("Error no controlado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Intente nuevamente en unos minutos.", null, null);
    }

    private ResponseEntity<RespuestaError> responder(HttpStatus estado, String mensaje, String regla,
                                                     Map<String, String> campos) {
        return ResponseEntity.status(estado)
                .body(new RespuestaError(estado.value(), mensaje, regla, campos, LocalDateTime.now()));
    }
}
