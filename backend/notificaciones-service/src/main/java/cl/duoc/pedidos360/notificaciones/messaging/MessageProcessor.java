package cl.duoc.pedidos360.notificaciones.messaging;

/** Logica de negocio que procesa el contenido de un mensaje ya deserializado. */
@FunctionalInterface
public interface MessageProcessor<T> {

    /**
     * @param payload contenido del mensaje
     * @param attempt numero de intento actual (1 = primer intento)
     */
    void process(T payload, int attempt) throws Exception;
}
