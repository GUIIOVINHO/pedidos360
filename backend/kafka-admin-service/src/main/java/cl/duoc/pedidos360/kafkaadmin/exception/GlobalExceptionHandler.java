package cl.duoc.pedidos360.kafkaadmin.exception;

import cl.duoc.pedidos360.kafkaadmin.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/** Traduce las excepciones a respuestas HTTP con mensajes claros (sin detalles internos de Kafka). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex) {
        return build(HttpStatus.BAD_REQUEST, mensajes(ex.getBindingResult()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleParamValidation(BindException ex) {
        return build(HttpStatus.BAD_REQUEST, mensajes(ex.getBindingResult()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST,
                List.of("El cuerpo de la peticion no es un JSON valido o contiene valores no permitidos"));
    }

    @ExceptionHandler({OperacionInvalidaException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiError> handleInvalid(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, List.of(ex.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> handleNotFound(RecursoNoEncontradoException ex) {
        return build(HttpStatus.NOT_FOUND, List.of(ex.getMessage()));
    }

    @ExceptionHandler(RecursoYaExisteException.class)
    public ResponseEntity<ApiError> handleConflict(RecursoYaExisteException ex) {
        return build(HttpStatus.CONFLICT, List.of(ex.getMessage()));
    }

    @ExceptionHandler(KafkaNoDisponibleException.class)
    public ResponseEntity<ApiError> handleUnavailable(KafkaNoDisponibleException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, List.of(ex.getMessage()));
    }

    private List<String> mensajes(BindingResult result) {
        List<String> mensajes = new ArrayList<>();
        result.getFieldErrors().forEach(fe -> mensajes.add(fe.getField() + ": " + fe.getDefaultMessage()));
        result.getGlobalErrors().forEach(ge -> mensajes.add(ge.getDefaultMessage()));
        return mensajes;
    }

    private ResponseEntity<ApiError> build(HttpStatus status, List<String> mensajes) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), status.getReasonPhrase(), mensajes));
    }
}
