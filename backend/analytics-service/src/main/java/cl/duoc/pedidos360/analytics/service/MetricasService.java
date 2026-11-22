package cl.duoc.pedidos360.analytics.service;

import cl.duoc.pedidos360.analytics.dto.EventoResumenDto;
import cl.duoc.pedidos360.analytics.dto.MetricasPedidosDto;
import cl.duoc.pedidos360.analytics.dto.PedidoEventoDto;
import cl.duoc.pedidos360.analytics.exception.EventoInvalidoException;
import cl.duoc.pedidos360.analytics.exception.ProcesamientoTransitorioException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * LOGICA DE NEGOCIO de las metricas en tiempo real. No conoce Kafka: recibe el evento ya
 * deserializado y mantiene los contadores en memoria (seguros para varios hilos, porque el
 * listener usa un hilo por particion).
 *
 * Ganchos de demostracion (campo "producto" o "cantidad" del pedido):
 *  - cantidad mayor a 1000 ....... evento invalido (no recuperable)  -> DLT directa
 *  - producto con "FALLO-DLQ" .... error transitorio permanente      -> reintentos y luego DLT
 */
@Service
public class MetricasService {

    private static final int CANTIDAD_MAXIMA = 1000;
    private static final int TOP = 10;
    private static final int MAX_ULTIMOS_EVENTOS = 10;
    private static final long VENTANA_MS = 60_000L;

    private final LongAdder totalPedidos = new LongAdder();
    private final LongAdder totalActualizaciones = new LongAdder();
    private final LongAdder totalUnidades = new LongAdder();
    private final Map<String, LongAdder> unidadesPorProducto = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> pedidosPorCliente = new ConcurrentHashMap<>();
    private final Queue<Long> marcasUltimoMinuto = new ConcurrentLinkedQueue<>();
    private final Deque<EventoResumenDto> ultimosEventos = new ConcurrentLinkedDeque<>();

    /** Valida el evento, actualiza las metricas y devuelve la foto resultante. */
    public MetricasPedidosDto registrar(PedidoEventoDto evento) {
        validar(evento);
        simularFallos(evento);

        if ("PEDIDO_CREADO".equals(evento.eventType())) {
            totalPedidos.increment();
            totalUnidades.add(evento.cantidad());
            unidadesPorProducto.computeIfAbsent(normalizar(evento.producto()), k -> new LongAdder()).add(evento.cantidad());
            pedidosPorCliente.computeIfAbsent(normalizar(evento.cliente()), k -> new LongAdder()).increment();
            marcasUltimoMinuto.add(System.currentTimeMillis());
        } else {
            totalActualizaciones.increment();
        }

        ultimosEventos.addFirst(new EventoResumenDto(evento.eventType(), evento.pedidoId(), evento.cliente(),
                evento.producto(), evento.cantidad(), Instant.now().toString()));
        while (ultimosEventos.size() > MAX_ULTIMOS_EVENTOS) {
            ultimosEventos.pollLast();
        }
        return snapshot();
    }

    /** Foto actual de las metricas. */
    public MetricasPedidosDto snapshot() {
        return new MetricasPedidosDto(
                totalPedidos.sum(),
                totalActualizaciones.sum(),
                totalUnidades.sum(),
                contarUltimoMinuto(),
                top(unidadesPorProducto),
                top(pedidosPorCliente),
                List.copyOf(ultimosEventos),
                Instant.now().toString());
    }

    private long contarUltimoMinuto() {
        long limite = System.currentTimeMillis() - VENTANA_MS;
        while (true) {
            Long cabeza = marcasUltimoMinuto.peek();
            if (cabeza == null || cabeza >= limite) {
                break;
            }
            marcasUltimoMinuto.poll();
        }
        return marcasUltimoMinuto.size();
    }

    private void validar(PedidoEventoDto e) {
        if (e.eventType() == null || e.eventType().isBlank()) {
            throw new EventoInvalidoException("Evento sin tipo (eventType)");
        }
        if (!"PEDIDO_CREADO".equals(e.eventType()) && !"PEDIDO_ACTUALIZADO".equals(e.eventType())) {
            throw new EventoInvalidoException("Tipo de evento desconocido: " + e.eventType());
        }
        if (e.pedidoId() == null) {
            throw new EventoInvalidoException("Evento sin pedidoId");
        }
        if (e.cliente() == null || e.cliente().isBlank() || e.producto() == null || e.producto().isBlank()) {
            throw new EventoInvalidoException("Evento sin cliente o producto (pedido " + e.pedidoId() + ")");
        }
        if (e.cantidad() == null || e.cantidad() <= 0) {
            throw new EventoInvalidoException("Cantidad invalida en el pedido " + e.pedidoId());
        }
        if (e.cantidad() > CANTIDAD_MAXIMA) {
            throw new EventoInvalidoException(
                    "Cantidad " + e.cantidad() + " supera el maximo permitido (" + CANTIDAD_MAXIMA + ")");
        }
    }

    private void simularFallos(PedidoEventoDto e) {
        if (e.producto().contains("FALLO-DLQ")) {
            throw new ProcesamientoTransitorioException("Servicio de metricas no disponible (simulado)");
        }
    }

    private String normalizar(String texto) {
        return texto.trim().toLowerCase();
    }

    private Map<String, Long> top(Map<String, LongAdder> origen) {
        return origen.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().sum(), a.getValue().sum()))
                .limit(TOP)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().sum(),
                        (x, y) -> x, LinkedHashMap::new));
    }
}
