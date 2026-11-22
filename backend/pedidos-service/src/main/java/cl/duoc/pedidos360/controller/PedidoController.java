package cl.duoc.pedidos360.controller;

import cl.duoc.pedidos360.dto.PedidoRequest;
import cl.duoc.pedidos360.entity.Pedido;
import cl.duoc.pedidos360.messaging.PedidoEventPublisher;
import cl.duoc.pedidos360.messaging.PedidoKafkaPublisher;
import cl.duoc.pedidos360.repository.PedidoRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST para el recurso "pedidos". Todas las rutas quedan protegidas por FirebaseAuthFilter.
 * Tras crear o actualizar un pedido se publica un evento (asincrono) mediante PedidoEventPublisher;
 * el controlador no conoce ningun detalle de RabbitMQ.
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoRepository pedidoRepository;
    private final PedidoEventPublisher eventPublisher;
    private final PedidoKafkaPublisher kafkaPublisher;

    public PedidoController(PedidoRepository pedidoRepository, PedidoEventPublisher eventPublisher,
                            PedidoKafkaPublisher kafkaPublisher) {
        this.pedidoRepository = pedidoRepository;
        this.eventPublisher = eventPublisher;
        this.kafkaPublisher = kafkaPublisher;
    }

    @GetMapping
    public List<Pedido> listar() {
        return pedidoRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> obtener(@PathVariable Long id) {
        return pedidoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Pedido> crear(@Valid @RequestBody PedidoRequest request,
                                         HttpServletRequest httpRequest) {
        Pedido pedido = new Pedido();
        pedido.setCliente(request.getCliente());
        pedido.setProducto(request.getProducto());
        pedido.setCantidad(request.getCantidad());
        pedido.setEstado(request.getEstado());
        Object uid = httpRequest.getAttribute("uid");
        pedido.setCreadoPor(uid != null ? uid.toString() : "desconocido");

        Pedido guardado = pedidoRepository.save(pedido);
        eventPublisher.publicarPedidoCreado(guardado);   // RabbitMQ: notificaciones y tickets
        kafkaPublisher.publicarPedidoCreado(guardado);   // Kafka: streaming y metricas
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pedido> actualizar(@PathVariable Long id,
                                              @Valid @RequestBody PedidoRequest request) {
        return pedidoRepository.findById(id)
                .map(pedido -> {
                    pedido.setCliente(request.getCliente());
                    pedido.setProducto(request.getProducto());
                    pedido.setCantidad(request.getCantidad());
                    if (request.getEstado() != null) {
                        pedido.setEstado(request.getEstado());
                    }
                    Pedido actualizado = pedidoRepository.save(pedido);
                    eventPublisher.publicarPedidoActualizado(actualizado);   // RabbitMQ
                    kafkaPublisher.publicarPedidoActualizado(actualizado);   // Kafka
                    return ResponseEntity.ok(actualizado);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!pedidoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        pedidoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
