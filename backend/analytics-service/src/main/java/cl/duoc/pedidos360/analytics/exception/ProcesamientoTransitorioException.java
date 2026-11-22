package cl.duoc.pedidos360.analytics.exception;

/** Error pasajero: se reintenta; si se agotan los reintentos el registro va a la DLT. */
public class ProcesamientoTransitorioException extends RuntimeException {
    public ProcesamientoTransitorioException(String message) {
        super(message);
    }
}
