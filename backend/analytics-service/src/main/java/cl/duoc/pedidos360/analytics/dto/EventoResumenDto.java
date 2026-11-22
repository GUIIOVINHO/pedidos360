package cl.duoc.pedidos360.analytics.dto;

/** Resumen de un evento reciente (para mostrar el flujo continuo en el dashboard). */
public record EventoResumenDto(String tipo, Long pedidoId, String cliente, String producto,
                               Integer cantidad, String recibidoEn) {
}
