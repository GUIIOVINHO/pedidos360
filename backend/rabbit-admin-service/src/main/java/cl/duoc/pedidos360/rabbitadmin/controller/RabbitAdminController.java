package cl.duoc.pedidos360.rabbitadmin.controller;

import cl.duoc.pedidos360.rabbitadmin.dto.BindingRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.BindingResponse;
import cl.duoc.pedidos360.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.ExchangeResponse;
import cl.duoc.pedidos360.rabbitadmin.dto.QueueRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.QueueResponse;
import cl.duoc.pedidos360.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * API REST de administracion de RabbitMQ. Todas las rutas estan protegidas por FirebaseAuthFilter (JWT).
 *
 *   POST   /api/queues                     crea una cola          (201)
 *   GET    /api/queues/{name}              consulta una cola      (200 / 404)
 *   DELETE /api/queues/{name}              elimina una cola       (204 / 404)
 *   POST   /api/exchanges                  crea un exchange       (201)
 *   DELETE /api/exchanges/{name}           elimina un exchange    (204)
 *   POST   /api/bindings                   crea un binding        (201)
 *   DELETE /api/bindings?queue=&exchange=&routingKey=   elimina un binding (204)
 */
@RestController
@RequestMapping("/api")
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    public RabbitAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    // ---- Colas ----

    @PostMapping("/queues")
    public ResponseEntity<QueueResponse> crearCola(@Valid @RequestBody QueueRequest request) {
        QueueResponse creada = rabbitAdminService.crearCola(request);
        return ResponseEntity.created(URI.create("/api/queues/" + creada.name())).body(creada);
    }

    @GetMapping("/queues/{name}")
    public QueueResponse obtenerCola(@PathVariable String name) {
        return rabbitAdminService.obtenerCola(name);
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> eliminarCola(@PathVariable String name) {
        rabbitAdminService.eliminarCola(name);
        return ResponseEntity.noContent().build();
    }

    // ---- Exchanges ----

    @PostMapping("/exchanges")
    public ResponseEntity<ExchangeResponse> crearExchange(@Valid @RequestBody ExchangeRequest request) {
        ExchangeResponse creado = rabbitAdminService.crearExchange(request);
        return ResponseEntity.created(URI.create("/api/exchanges/" + creado.name())).body(creado);
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String name) {
        rabbitAdminService.eliminarExchange(name);
        return ResponseEntity.noContent().build();
    }

    // ---- Bindings ----

    @PostMapping("/bindings")
    public ResponseEntity<BindingResponse> crearBinding(@Valid @RequestBody BindingRequest request) {
        BindingResponse creado = rabbitAdminService.crearBinding(request);
        return ResponseEntity.status(201).body(creado);
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid BindingRequest request) {
        rabbitAdminService.eliminarBinding(request);
        return ResponseEntity.noContent().build();
    }
}
