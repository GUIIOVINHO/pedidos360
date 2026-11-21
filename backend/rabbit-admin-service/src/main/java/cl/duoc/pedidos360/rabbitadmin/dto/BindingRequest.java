package cl.duoc.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de un binding cola <- exchange. Se usa como cuerpo JSON al crear (POST)
 * y como parametros de consulta (?queue=&exchange=&routingKey=) al eliminar (DELETE).
 */
public class BindingRequest {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    @Size(max = ValidationPatterns.MAX_LONGITUD, message = "El nombre de la cola no puede superar los 255 caracteres")
    @Pattern(regexp = ValidationPatterns.NOMBRE_REFERENCIA, message = "El nombre de la cola contiene caracteres no permitidos")
    private String queue;

    @NotBlank(message = "El nombre del exchange es obligatorio")
    @Size(max = ValidationPatterns.MAX_LONGITUD, message = "El nombre del exchange no puede superar los 255 caracteres")
    @Pattern(regexp = ValidationPatterns.NOMBRE_REFERENCIA, message = "El nombre del exchange contiene caracteres no permitidos")
    private String exchange;

    @Size(max = ValidationPatterns.MAX_LONGITUD, message = "La routing key no puede superar los 255 caracteres")
    private String routingKey = "";

    public String getQueue() { return queue; }
    public void setQueue(String queue) { this.queue = queue; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey == null ? "" : routingKey; }
}
