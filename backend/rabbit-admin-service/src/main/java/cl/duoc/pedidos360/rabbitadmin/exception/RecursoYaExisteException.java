package cl.duoc.pedidos360.rabbitadmin.exception;

public class RecursoYaExisteException extends RuntimeException {
    public RecursoYaExisteException(String message) {
        super(message);
    }
}
