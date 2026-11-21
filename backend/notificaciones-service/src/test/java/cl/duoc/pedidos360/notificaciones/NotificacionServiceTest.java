package cl.duoc.pedidos360.notificaciones;

import cl.duoc.pedidos360.notificaciones.dto.PedidoEvent;
import cl.duoc.pedidos360.notificaciones.exception.NonRecoverableMessageException;
import cl.duoc.pedidos360.notificaciones.exception.RecoverableProcessingException;
import cl.duoc.pedidos360.notificaciones.service.NotificacionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificacionServiceTest {

    private final NotificacionService service = new NotificacionService();

    private PedidoEvent evento(String cliente, String producto, int cantidad) {
        return new PedidoEvent("PEDIDO_CREADO", 1L, cliente, producto, cantidad, "PENDIENTE", "uid-1", "2026-01-01T00:00:00Z");
    }

    @Test
    void procesaUnPedidoValido() {
        assertDoesNotThrow(() -> service.procesar(evento("Ana", "Mouse", 2), 1));
    }

    @Test
    void cantidadExcesivaEsErrorNoRecuperable() {
        assertThrows(NonRecoverableMessageException.class,
                () -> service.procesar(evento("Ana", "Mouse", 5000), 1));
    }

    @Test
    void fallaTemporalSeRecuperaAlTercerIntento() {
        assertThrows(RecoverableProcessingException.class,
                () -> service.procesar(evento("Ana", "FALLO-TEMPORAL", 1), 1));
        assertDoesNotThrow(() -> service.procesar(evento("Ana", "FALLO-TEMPORAL", 1), 3));
    }
}
