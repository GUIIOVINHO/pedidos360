package cl.duoc.pedidos360.kafkaadmin.exception;

public class RecursoYaExisteException extends RuntimeException {
    public RecursoYaExisteException(String message) {
        super(message);
    }
}
