package cl.duoc.pedidos360.analytics;

import cl.duoc.pedidos360.analytics.dto.MetricasPedidosDto;
import cl.duoc.pedidos360.analytics.dto.PedidoEventoDto;
import cl.duoc.pedidos360.analytics.exception.EventoInvalidoException;
import cl.duoc.pedidos360.analytics.exception.ProcesamientoTransitorioException;
import cl.duoc.pedidos360.analytics.service.MetricasService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetricasServiceTest {

    private PedidoEventoDto evento(String tipo, String producto, int cantidad) {
        return new PedidoEventoDto(tipo, 1L, "Ana", producto, cantidad, "PENDIENTE", "uid-1", "2026-01-01T00:00:00Z");
    }

    @Test
    void unPedidoCreadoActualizaLasMetricas() {
        MetricasService service = new MetricasService();
        MetricasPedidosDto snapshot = service.registrar(evento("PEDIDO_CREADO", "Mouse", 3));

        assertEquals(1, snapshot.totalPedidos());
        assertEquals(3, snapshot.totalUnidades());
        assertEquals(1, snapshot.pedidosUltimoMinuto());
        assertEquals(3L, snapshot.unidadesPorProducto().get("mouse"));
    }

    @Test
    void unaActualizacionNoSumaPedidosNuevos() {
        MetricasService service = new MetricasService();
        MetricasPedidosDto snapshot = service.registrar(evento("PEDIDO_ACTUALIZADO", "Mouse", 3));

        assertEquals(0, snapshot.totalPedidos());
        assertEquals(1, snapshot.totalActualizaciones());
    }

    @Test
    void cantidadExcesivaEsEventoInvalido() {
        MetricasService service = new MetricasService();
        assertThrows(EventoInvalidoException.class, () -> service.registrar(evento("PEDIDO_CREADO", "Mouse", 5000)));
    }

    @Test
    void falloDlqEsErrorTransitorio() {
        MetricasService service = new MetricasService();
        assertThrows(ProcesamientoTransitorioException.class,
                () -> service.registrar(evento("PEDIDO_CREADO", "FALLO-DLQ", 1)));
    }
}
