package cl.duoc.pedidos360.rabbitadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos para crear una cola. Todas las reglas se validan antes de llegar al servicio. */
public class QueueRequest {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    @Size(max = ValidationPatterns.MAX_LONGITUD, message = "El nombre de la cola no puede superar los 255 caracteres")
    @Pattern(regexp = ValidationPatterns.NOMBRE_CREABLE,
            message = "Nombre invalido: solo letras, numeros, '.', '_' y '-', sin espacios y sin el prefijo reservado 'amq.'")
    private String name;

    private boolean durable = true;

    private boolean autoDelete = false;

    /** Opcional: exchange al que se envian los mensajes rechazados (dead letter). */
    @Size(max = ValidationPatterns.MAX_LONGITUD, message = "deadLetterExchange es demasiado largo")
    @Pattern(regexp = ValidationPatterns.NOMBRE_REFERENCIA, message = "deadLetterExchange contiene caracteres no permitidos")
    private String deadLetterExchange;

    /** Opcional: routing key con la que se re-enrutan los mensajes rechazados. */
    @Size(max = ValidationPatterns.MAX_LONGITUD, message = "deadLetterRoutingKey es demasiado largo")
    private String deadLetterRoutingKey;

    /** Opcional: tiempo de vida de los mensajes en la cola, en milisegundos. */
    @Min(value = 1, message = "messageTtlMs debe ser mayor a 0")
    private Long messageTtlMs;

    @JsonIgnore
    @AssertTrue(message = "deadLetterRoutingKey requiere indicar deadLetterExchange")
    public boolean isDeadLetterConfigValida() {
        boolean tieneKey = deadLetterRoutingKey != null && !deadLetterRoutingKey.isBlank();
        boolean tieneExchange = deadLetterExchange != null && !deadLetterExchange.isBlank();
        return !tieneKey || tieneExchange;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isDurable() { return durable; }
    public void setDurable(boolean durable) { this.durable = durable; }

    public boolean isAutoDelete() { return autoDelete; }
    public void setAutoDelete(boolean autoDelete) { this.autoDelete = autoDelete; }

    public String getDeadLetterExchange() { return deadLetterExchange; }
    public void setDeadLetterExchange(String deadLetterExchange) { this.deadLetterExchange = deadLetterExchange; }

    public String getDeadLetterRoutingKey() { return deadLetterRoutingKey; }
    public void setDeadLetterRoutingKey(String deadLetterRoutingKey) { this.deadLetterRoutingKey = deadLetterRoutingKey; }

    public Long getMessageTtlMs() { return messageTtlMs; }
    public void setMessageTtlMs(Long messageTtlMs) { this.messageTtlMs = messageTtlMs; }
}
