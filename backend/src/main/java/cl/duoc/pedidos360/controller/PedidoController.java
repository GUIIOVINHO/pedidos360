package cl.duoc.pedidos360.controller;

import cl.duoc.pedidos360.dto.PedidoRequest;
import cl.duoc.pedidos360.entity.Pedido;
import cl.duoc.pedidos360.repository.PedidoRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST (arquitectural style: REST sobre HTTP/JSON) para el recurso "pedidos".
 * Todas las rutas quedan protegidas por FirebaseAuthFilter: solo se llega
 * hasta aqui si el JWT (ID Token de Firebase) enviado en el header
 * "Authorization: Bearer <token>" es valido.
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoRepository pedidoRepository;

    public PedidoController(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
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
        // El filtro de seguridad deja el uid del usuario (extraido del JWT)
        // disponible como atributo del request.
        Object uid = httpRequest.getAttribute("uid");
        pedido.setCreadoPor(uid != null ? uid.toString() : "desconocido");

        Pedido guardado = pedidoRepository.save(pedido);
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
                    return ResponseEntity.ok(pedidoRepository.save(pedido));
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
