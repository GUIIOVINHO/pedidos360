package cl.duoc.pedidos360.kafkaadmin.dto;

/** Politicas de limpieza de un topico. */
public enum CleanupPolicy {
    DELETE("delete"),
    COMPACT("compact"),
    COMPACT_DELETE("compact,delete");

    private final String valor;

    CleanupPolicy(String valor) {
        this.valor = valor;
    }

    public String valor() {
        return valor;
    }
}
