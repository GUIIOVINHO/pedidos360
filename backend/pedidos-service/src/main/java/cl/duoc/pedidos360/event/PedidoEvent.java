package cl.duoc.pedidos360.event;

/**
 * Evento de dominio que se publica en RabbitMQ cuando un pedido se crea o se actualiza.
 * Es el "contrato" del mensaje entre el productor y los consumidores.
 */
public record PedidoEvent(
        String eventType,
        Long pedidoId,
        String cliente,
        String producto,
        Integer cantidad,
        String estado,
        String creadoPor,
        String occurredAt) {
}
