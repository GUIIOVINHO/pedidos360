package cl.duoc.pedidos360.notificaciones.dto;

/** Foto de metricas publicada por analytics-service en pedidos.metricas (solo los campos que este servicio usa). */
public record MetricasPedidosDto(
        long totalPedidos,
        long totalUnidades,
        long pedidosUltimoMinuto,
        String actualizadoEn) {
}
