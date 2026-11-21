package cl.duoc.pedidos360.rabbitadmin.exception;

import cl.duoc.pedidos360.rabbitadmin.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
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

/**
 * Traduce las excepciones a respuestas HTTP con mensajes claros.
 * Nunca se expone el mensaje interno de una excepcion de RabbitMQ al cliente (solo se registra en el log).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex) {
        return badRequest(mensajes(ex.getBindingResult()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleParamValidation(BindException ex) {
        return badRequest(mensajes(ex.getBindingResult()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return badRequest(List.of("El cuerpo de la peticion no es un JSON valido o contiene valores no permitidos"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return badRequest(List.of(ex.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> handleNotFound(RecursoNoEncontradoException ex) {
        return build(HttpStatus.NOT_FOUND, List.of(ex.getMessage()));
    }

    @ExceptionHandler(RecursoYaExisteException.class)
    public ResponseEntity<ApiError> handleConflict(RecursoYaExisteException ex) {
        return build(HttpStatus.CONFLICT, List.of(ex.getMessage()));
    }

    @ExceptionHandler(AmqpConnectException.class)
    public ResponseEntity<ApiError> handleBrokerDown(AmqpConnectException ex) {
        log.error("RabbitMQ no disponible: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, List.of("RabbitMQ no esta disponible en este momento"));
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ApiError> handleAmqp(AmqpException ex) {
        log.error("RabbitMQ rechazo la operacion: {}", ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, List.of(
                "RabbitMQ rechazo la operacion (revise que el exchange exista y que los parametros no contradigan una declaracion previa)"));
    }

    private List<String> mensajes(BindingResult result) {
        List<String> mensajes = new ArrayList<>();
        result.getFieldErrors().forEach(fe -> mensajes.add(fe.getField() + ": " + fe.getDefaultMessage()));
        result.getGlobalErrors().forEach(ge -> mensajes.add(ge.getDefaultMessage()));
        return mensajes;
    }

    private ResponseEntity<ApiError> badRequest(List<String> mensajes) {
        return build(HttpStatus.BAD_REQUEST, mensajes);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, List<String> mensajes) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), status.getReasonPhrase(), mensajes));
    }
}
