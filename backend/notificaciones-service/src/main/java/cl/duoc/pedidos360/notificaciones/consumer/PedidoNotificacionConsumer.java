package cl.duoc.pedidos360.notificaciones.consumer;

import cl.duoc.pedidos360.notificaciones.dto.PedidoEvent;
import cl.duoc.pedidos360.notificaciones.messaging.MessageAckSupport;
import cl.duoc.pedidos360.notificaciones.service.NotificacionService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * CONSUMIDOR del dominio "notificaciones". Solo adapta el mensaje de RabbitMQ hacia el servicio
 * de negocio; el ACK, los reintentos y el envio a DLQ los resuelve MessageAckSupport.
 */
@Component
public class PedidoNotificacionConsumer {

    private final MessageAckSupport ackSupport;
    private final NotificacionService notificacionService;

    public PedidoNotificacionConsumer(MessageAckSupport ackSupport, NotificacionService notificacionService) {
        this.ackSupport = ackSupport;
        this.notificacionService = notificacionService;
    }

    @RabbitListener(queues = "${app.rabbit.queues.notificaciones}")
    public void onPedidoEvent(Message message, Channel channel) {
        ackSupport.handle(message, channel, PedidoEvent.class, notificacionService::procesar);
    }
}
