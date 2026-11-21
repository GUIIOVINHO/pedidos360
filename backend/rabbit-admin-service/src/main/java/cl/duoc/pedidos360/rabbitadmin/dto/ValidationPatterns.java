package cl.duoc.pedidos360.rabbitadmin.dto;

/** Expresiones regulares compartidas para validar nombres de recursos de RabbitMQ. */
public final class ValidationPatterns {

    /** Nombre de cola/exchange que se puede crear: sin espacios y sin el prefijo reservado "amq.". */
    public static final String NOMBRE_CREABLE = "^(?!amq\\.)[A-Za-z0-9._-]*$";

    /** Nombre de recurso existente al que se puede referenciar (permite exchanges "amq.*"). */
    public static final String NOMBRE_REFERENCIA = "^[A-Za-z0-9._-]*$";

    public static final int MAX_LONGITUD = 255;

    private ValidationPatterns() {
    }
}
