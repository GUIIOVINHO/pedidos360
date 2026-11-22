package cl.duoc.pedidos360.kafkaadmin.exception;

public class OperacionInvalidaException extends RuntimeException {
    public OperacionInvalidaException(String message) {
        super(message);
    }
}
