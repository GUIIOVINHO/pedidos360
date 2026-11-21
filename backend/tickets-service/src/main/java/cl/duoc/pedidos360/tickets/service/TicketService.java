package cl.duoc.pedidos360.tickets.service;

import cl.duoc.pedidos360.tickets.dto.PedidoEvent;
import cl.duoc.pedidos360.tickets.exception.NonRecoverableMessageException;
import cl.duoc.pedidos360.tickets.exception.RecoverableProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Logica de negocio de la generacion de tickets. NO conoce RabbitMQ: recibe el evento ya
 * deserializado y genera un archivo ticket-{pedidoId}.txt en la carpeta configurada.
 *
 * Ganchos de demostracion (igual que en notificaciones-service):
 *  - cantidad mayor a 1000 ........ error NO recuperable  -> DLQ inmediata
 *  - producto con "FALLO-TEMPORAL" . falla en los 2 primeros intentos y funciona al 3ro
 *  - producto con "FALLO-DLQ" ...... falla siempre -> se agotan los reintentos -> DLQ
 */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final int CANTIDAD_MAXIMA = 1000;

    private final Path outputDir;

    public TicketService(@Value("${app.tickets.output-dir:./tickets}") String outputDir) {
        this.outputDir = Path.of(outputDir);
    }

    public void procesar(PedidoEvent event, int intento) {
        validar(event);
        simularFallos(event, intento);

        String contenido = """
                ===== TICKET PEDIDOS360 =====
                N° ticket : TK-%06d
                Pedido    : #%d
                Cliente   : %s
                Producto  : %s
                Cantidad  : %d
                Estado    : %s
                Emitido   : %s
                ==============================
                """.formatted(event.pedidoId(), event.pedidoId(), event.cliente(), event.producto(),
                event.cantidad(), event.estado(), LocalDateTime.now());

        try {
            Files.createDirectories(outputDir);
            Path archivo = outputDir.resolve("ticket-" + event.pedidoId() + ".txt");
            Files.writeString(archivo, contenido, StandardCharsets.UTF_8);
            log.info("[TICKET] Ticket generado para el pedido #{}: {}", event.pedidoId(), archivo.toAbsolutePath());
        } catch (IOException ex) {
            throw new RecoverableProcessingException("No se pudo escribir el ticket: " + ex.getMessage());
        }
    }

    private void validar(PedidoEvent event) {
        if (event.pedidoId() == null || event.cliente() == null || event.cliente().isBlank()
                || event.producto() == null || event.cantidad() == null) {
            throw new NonRecoverableMessageException("Evento incompleto: no se puede generar el ticket");
        }
        if (event.cantidad() > CANTIDAD_MAXIMA) {
            throw new NonRecoverableMessageException(
                    "Cantidad " + event.cantidad() + " supera el maximo permitido (" + CANTIDAD_MAXIMA + ")");
        }
    }

    private void simularFallos(PedidoEvent event, int intento) {
        String producto = event.producto() == null ? "" : event.producto();
        if (producto.contains("FALLO-DLQ")) {
            throw new RecoverableProcessingException("Generador de documentos no disponible (simulado)");
        }
        if (producto.contains("FALLO-TEMPORAL") && intento <= 2) {
            throw new RecoverableProcessingException("Fallo temporal simulado (intento " + intento + ")");
        }
    }
}
