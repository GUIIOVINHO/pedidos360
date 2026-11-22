package cl.duoc.pedidos360.notificaciones;

import cl.duoc.pedidos360.notificaciones.dto.MetricasPedidosDto;
import cl.duoc.pedidos360.notificaciones.exception.EventoInvalidoException;
import cl.duoc.pedidos360.notificaciones.service.AlertaDemandaService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertaDemandaServiceTest {

    private final AlertaDemandaService service = new AlertaDemandaService(5);

    @Test
    void sinAlertaBajoElUmbral() {
        assertFalse(service.evaluar(new MetricasPedidosDto(10, 20, 2, "2026-01-01T00:00:00Z")));
    }

    @Test
    void alertaAlSuperarElUmbral() {
        assertTrue(service.evaluar(new MetricasPedidosDto(10, 20, 5, "2026-01-01T00:00:00Z")));
    }

    @Test
    void metricasNulasSonInvalidas() {
        assertThrows(EventoInvalidoException.class, () -> service.evaluar(null));
    }
}
