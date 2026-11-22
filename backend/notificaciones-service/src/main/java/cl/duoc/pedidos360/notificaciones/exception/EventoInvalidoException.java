package cl.duoc.pedidos360.notificaciones.exception;

/** La metrica recibida es invalida: reintentar no sirve, va directo a la DLT de Kafka. */
public class EventoInvalidoException extends RuntimeException {
    public EventoInvalidoException(String message) {
        super(message);
    }
}
