package cl.duoc.pedidos360.tickets.dto;

/** Evento de pedido tal como lo publica pedidos-service (contrato del mensaje). */
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
