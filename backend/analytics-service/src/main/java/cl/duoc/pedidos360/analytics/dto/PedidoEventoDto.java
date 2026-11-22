package cl.duoc.pedidos360.analytics.dto;

/** Evento de pedido tal como lo publica pedidos-service en Kafka (contrato del mensaje). */
public record PedidoEventoDto(
        String eventType,
        Long pedidoId,
        String cliente,
        String producto,
        Integer cantidad,
        String estado,
        String creadoPor,
        String occurredAt) {
}
