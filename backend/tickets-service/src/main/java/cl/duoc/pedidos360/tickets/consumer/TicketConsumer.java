package cl.duoc.pedidos360.tickets.consumer;

import cl.duoc.pedidos360.tickets.dto.PedidoEvent;
import cl.duoc.pedidos360.tickets.messaging.MessageAckSupport;
import cl.duoc.pedidos360.tickets.service.TicketService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * CONSUMIDOR del dominio "tickets". Solo adapta el mensaje de RabbitMQ hacia el servicio
 * de negocio; el ACK, los reintentos y el envio a DLQ los resuelve MessageAckSupport.
 */
@Component
public class TicketConsumer {

    private final MessageAckSupport ackSupport;
    private final TicketService ticketService;

    public TicketConsumer(MessageAckSupport ackSupport, TicketService ticketService) {
        this.ackSupport = ackSupport;
        this.ticketService = ticketService;
    }

    @RabbitListener(queues = "${app.rabbit.queues.tickets}")
    public void onPedidoCreado(Message message, Channel channel) {
        ackSupport.handle(message, channel, PedidoEvent.class, ticketService::procesar);
    }
}
