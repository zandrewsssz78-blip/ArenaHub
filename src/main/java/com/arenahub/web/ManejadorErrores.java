package com.arenahub.web;

import com.arenahub.aplicacion.AutenticacionException;
import com.arenahub.aplicacion.ConflictoException;
import com.arenahub.aplicacion.RecursoNoEncontradoException;
import com.arenahub.dominio.ReglaNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Traduce las excepciones a respuestas HTTP con un cuerpo uniforme.
 * Regla 4 & SEG-12: Enmascara errores y nunca expone stack traces ni detalles internos al cliente.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    public record ErrorResponse(int status, String error, String mensaje) {
    }

    @ExceptionHandler(AutenticacionException.class)
    public ResponseEntity<ErrorResponse> autenticacion(AutenticacionException e) {
        return respuesta(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> conflicto(ConflictoException e) {
        return respuesta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> reglaNegocio(ReglaNegocioException e) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return respuesta(HttpStatus.BAD_REQUEST, detalle);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> solicitudInvalida(Exception e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Solicitud inválida");
    }

    /**
     * Regla 4 & SEG-12: Captura cualquier excepción no controlada para evitar
     * que se filtre la traza interna (stack trace) o detalles de la base de datos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> errorInternoNoControlado(Exception e) {
        log.error("Excepción no controlada capturada por ManejadorErrores: {}", e.getMessage(), e);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error interno. Intente más tarde.");
    }

    private static ResponseEntity<ErrorResponse> respuesta(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), mensaje));
    }
}
