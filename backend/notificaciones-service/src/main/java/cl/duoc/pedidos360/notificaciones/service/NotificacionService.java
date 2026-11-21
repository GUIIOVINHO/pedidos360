package cl.duoc.pedidos360.notificaciones.service;

import cl.duoc.pedidos360.notificaciones.dto.PedidoEvent;
import cl.duoc.pedidos360.notificaciones.exception.NonRecoverableMessageException;
import cl.duoc.pedidos360.notificaciones.exception.RecoverableProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Logica de negocio de las notificaciones. NO conoce RabbitMQ: solo recibe el evento ya
 * deserializado. Aqui se simula el envio (correo/push) escribiendo en el log.
 *
 * Ganchos de demostracion para la presentacion (usar en el campo "producto" o "cantidad"):
 *  - cantidad mayor a 1000 ........ error NO recuperable  -> DLQ inmediata
 *  - producto con "FALLO-TEMPORAL" . falla en los 2 primeros intentos y funciona al 3ro (reintentos)
 *  - producto con "FALLO-DLQ" ...... falla siempre -> se agotan los reintentos -> DLQ
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final int CANTIDAD_MAXIMA = 1000;

    public void procesar(PedidoEvent event, int intento) {
        validar(event);
        simularFallos(event, intento);

        String detalle = "PEDIDO_CREADO".equals(event.eventType())
                ? "fue registrado correctamente"
                : "cambio de estado a " + event.estado();
        log.info("[NOTIFICACION] Para {}: tu pedido #{} ({} x {}) {}.",
                event.cliente(), event.pedidoId(), event.cantidad(), event.producto(), detalle);
    }

    private void validar(PedidoEvent event) {
        if (event.pedidoId() == null || event.cliente() == null || event.cliente().isBlank()) {
            throw new NonRecoverableMessageException("Evento incompleto: faltan pedidoId o cliente");
        }
        if (event.cantidad() != null && event.cantidad() > CANTIDAD_MAXIMA) {
            throw new NonRecoverableMessageException(
                    "Cantidad " + event.cantidad() + " supera el maximo permitido (" + CANTIDAD_MAXIMA + ")");
        }
    }

    private void simularFallos(PedidoEvent event, int intento) {
        String producto = event.producto() == null ? "" : event.producto();
        if (producto.contains("FALLO-DLQ")) {
            throw new RecoverableProcessingException("Servicio de notificaciones no disponible (simulado)");
        }
        if (producto.contains("FALLO-TEMPORAL") && intento <= 2) {
            throw new RecoverableProcessingException("Fallo temporal simulado (intento " + intento + ")");
        }
    }
}
