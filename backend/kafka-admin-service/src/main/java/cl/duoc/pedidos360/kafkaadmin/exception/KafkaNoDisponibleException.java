package cl.duoc.pedidos360.kafkaadmin.exception;

public class KafkaNoDisponibleException extends RuntimeException {
    public KafkaNoDisponibleException(String message) {
        super(message);
    }
}
