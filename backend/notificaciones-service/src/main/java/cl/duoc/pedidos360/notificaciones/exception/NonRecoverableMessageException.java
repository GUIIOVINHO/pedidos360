package cl.duoc.pedidos360.notificaciones.exception;

/**
 * Error que NO se arregla reintentando (mensaje invalido, regla de negocio violada).
 * El consumidor lo envia directo a la DLQ (NACK sin requeue), sin reintentos.
 */
public class NonRecoverableMessageException extends RuntimeException {
    public NonRecoverableMessageException(String message) {
        super(message);
    }
}
