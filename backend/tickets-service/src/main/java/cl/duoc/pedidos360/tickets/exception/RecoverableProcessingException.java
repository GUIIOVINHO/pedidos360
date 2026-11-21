package cl.duoc.pedidos360.tickets.exception;

/**
 * Error transitorio (ej. un servicio externo caido). El consumidor reintenta el mensaje
 * hasta el maximo configurado; si se agotan los reintentos, termina en la DLQ.
 */
public class RecoverableProcessingException extends RuntimeException {
    public RecoverableProcessingException(String message) {
        super(message);
    }
}
