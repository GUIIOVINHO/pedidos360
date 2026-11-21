package cl.duoc.pedidos360.rabbitadmin.dto;

public record BindingResponse(String queue, String exchange, String routingKey) {
}
