package cl.duoc.pedidos360.messaging;

import cl.duoc.pedidos360.config.KafkaTopicsProperties;
import cl.duoc.pedidos360.entity.Pedido;
import cl.duoc.pedidos360.event.PedidoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * PRODUCTOR de Kafka. Publica cada evento de pedido en el topico configurado (pedidos.eventos),
 * serializado como JSON a partir del DTO PedidoEvent. La clave es el id del pedido.
 *
 * Igual que con RabbitMQ, un fallo de Kafka solo se registra en el log: el pedido ya fue guardado
 * y la API sigue respondiendo (max.block.ms=3000 evita que una caida de Kafka deje la peticion colgada).
 * RabbitMQ (notificaciones y tickets) y Kafka (streaming/analitica) funcionan en paralelo e independientes.
 */
@Component
public class PedidoKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoKafkaPublisher.class);

    private final KafkaTemplate<String, PedidoEvent> kafkaTemplate;
    private final KafkaTopicsProperties props;

    public PedidoKafkaPublisher(KafkaTemplate<String, PedidoEvent> kafkaTemplate, KafkaTopicsProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    public void publicarPedidoCreado(Pedido pedido) {
        publicar("PEDIDO_CREADO", pedido);
    }

    public void publicarPedidoActualizado(Pedido pedido) {
        publicar("PEDIDO_ACTUALIZADO", pedido);
    }

    private void publicar(String eventType, Pedido pedido) {
        PedidoEvent event = new PedidoEvent(
                eventType,
                pedido.getId(),
                pedido.getCliente(),
                pedido.getProducto(),
                pedido.getCantidad(),
                pedido.getEstado(),
                pedido.getCreadoPor(),
                Instant.now().toString());
        String topic = props.topics().eventos().name();
        try {
            kafkaTemplate.send(topic, String.valueOf(pedido.getId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("[KAFKA] No se pudo publicar {} del pedido {} en '{}': {}",
                                    eventType, pedido.getId(), topic, ex.getMessage());
                        } else {
                            log.info("[KAFKA] {} publicado (pedidoId={}, topico={}, particion={}, offset={})",
                                    eventType, pedido.getId(), topic,
                                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception ex) {
            log.error("[KAFKA] Error al enviar {} del pedido {}: {}. El pedido quedo guardado igualmente.",
                    eventType, pedido.getId(), ex.getMessage());
        }
    }
}
