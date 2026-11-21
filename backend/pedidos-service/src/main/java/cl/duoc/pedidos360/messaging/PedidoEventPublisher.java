package cl.duoc.pedidos360.messaging;

import cl.duoc.pedidos360.config.RabbitNamesProperties;
import cl.duoc.pedidos360.entity.Pedido;
import cl.duoc.pedidos360.event.PedidoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * PRODUCTOR de RabbitMQ. Publica eventos de pedidos en el exchange configurado.
 *
 * Importante (desacoplamiento): si RabbitMQ no esta disponible, el error se registra en el log
 * pero NO se propaga: el pedido ya fue guardado en la base de datos y la API sigue funcionando
 * igual que antes de incorporar la mensajeria.
 */
@Component
public class PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitNamesProperties props;

    public PedidoEventPublisher(RabbitTemplate rabbitTemplate, RabbitNamesProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public void publicarPedidoCreado(Pedido pedido) {
        publicar(props.routingKeys().pedidoCreado(), "PEDIDO_CREADO", pedido);
    }

    public void publicarPedidoActualizado(Pedido pedido) {
        publicar(props.routingKeys().pedidoActualizado(), "PEDIDO_ACTUALIZADO", pedido);
    }

    private void publicar(String routingKey, String eventType, Pedido pedido) {
        PedidoEvent event = new PedidoEvent(
                eventType,
                pedido.getId(),
                pedido.getCliente(),
                pedido.getProducto(),
                pedido.getCantidad(),
                pedido.getEstado(),
                pedido.getCreadoPor(),
                Instant.now().toString());
        try {
            rabbitTemplate.convertAndSend(props.exchanges().pedidos(), routingKey, event, message -> {
                message.getMessageProperties().setMessageId(UUID.randomUUID().toString());
                return message;
            });
            log.info("Evento {} publicado (pedidoId={}, routingKey={})", eventType, pedido.getId(), routingKey);
        } catch (AmqpException ex) {
            log.error("No se pudo publicar el evento {} del pedido {}. El pedido quedo guardado igualmente. Causa: {}",
                    eventType, pedido.getId(), ex.getMessage());
        }
    }
}
