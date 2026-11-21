package cl.duoc.pedidos360.rabbitadmin.dto;

public record QueueResponse(String name, int messageCount, int consumerCount) {
}
