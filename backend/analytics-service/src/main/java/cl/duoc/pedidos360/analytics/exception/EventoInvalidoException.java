package cl.duoc.pedidos360.analytics.exception;

/** El evento es invalido o rompe una regla de negocio: reintentar no sirve, va directo a la DLT. */
public class EventoInvalidoException extends RuntimeException {
    public EventoInvalidoException(String message) {
        super(message);
    }
}
