package cl.duoc.pedidos360.tickets;

import cl.duoc.pedidos360.tickets.dto.PedidoEvent;
import cl.duoc.pedidos360.tickets.exception.NonRecoverableMessageException;
import cl.duoc.pedidos360.tickets.exception.RecoverableProcessingException;
import cl.duoc.pedidos360.tickets.service.TicketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketServiceTest {

    @TempDir
    Path tempDir;

    private PedidoEvent evento(String producto, int cantidad) {
        return new PedidoEvent("PEDIDO_CREADO", 7L, "Ana", producto, cantidad, "PENDIENTE", "uid-1", "2026-01-01T00:00:00Z");
    }

    @Test
    void generaElArchivoDelTicket() {
        TicketService service = new TicketService(tempDir.toString());
        service.procesar(evento("Mouse", 2), 1);
        assertTrue(Files.exists(tempDir.resolve("ticket-7.txt")));
    }

    @Test
    void cantidadExcesivaEsErrorNoRecuperable() {
        TicketService service = new TicketService(tempDir.toString());
        assertThrows(NonRecoverableMessageException.class, () -> service.procesar(evento("Mouse", 5000), 1));
    }

    @Test
    void falloDlqSiempreEsRecuperable() {
        TicketService service = new TicketService(tempDir.toString());
        assertThrows(RecoverableProcessingException.class, () -> service.procesar(evento("FALLO-DLQ", 1), 3));
    }
}
