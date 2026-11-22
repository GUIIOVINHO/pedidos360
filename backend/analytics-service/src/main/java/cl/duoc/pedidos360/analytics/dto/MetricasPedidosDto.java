package cl.duoc.pedidos360.analytics.dto;

import java.util.List;
import java.util.Map;

/**
 * Foto de las metricas en tiempo real. Se publica en el topico pedidos.metricas (clave "global")
 * y tambien se expone por REST (GET /api/metricas) para el dashboard.
 */
public record MetricasPedidosDto(
        long totalPedidos,
        long totalActualizaciones,
        long totalUnidades,
        long pedidosUltimoMinuto,
        Map<String, Long> unidadesPorProducto,
        Map<String, Long> pedidosPorCliente,
        List<EventoResumenDto> ultimosEventos,
        String actualizadoEn) {
}
