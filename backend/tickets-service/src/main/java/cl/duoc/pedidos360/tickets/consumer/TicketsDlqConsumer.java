package cl.duoc.pedidos360.tickets.consumer;

import cl.duoc.pedidos360.tickets.messaging.MessageAckSupport;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Registra en el log cada mensaje NO entregado que llega a la DLQ de tickets.
 * Se puede desactivar con app.rabbit.dlq-consumer-enabled=false para dejar los mensajes
 * en la cola y revisarlos desde el panel de RabbitMQ.
 */
@Component
@ConditionalOnProperty(name = "app.rabbit.dlq-consumer-enabled", havingValue = "true", matchIfMissing = true)
public class TicketsDlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(TicketsDlqConsumer.class);

    private final MessageAckSupport ackSupport;

    public TicketsDlqConsumer(MessageAckSupport ackSupport) {
        this.ackSupport = ackSupport;
    }

    @RabbitListener(queues = "${app.rabbit.queues.tickets-dlq}")
    public void onDeadLetter(Message message, Channel channel) {
        MessageProperties mp = message.getMessageProperties();
        log.warn("[DLQ] Mensaje NO entregado | motivo={} | reintentos={} | cola origen={} | cuerpo={}",
                mp.getHeader("x-first-death-reason"),
                mp.getHeader(MessageAckSupport.RETRY_HEADER) == null ? 0 : mp.getHeader(MessageAckSupport.RETRY_HEADER),
                mp.getHeader("x-first-death-queue"),
                new String(message.getBody(), StandardCharsets.UTF_8));
        ackSupport.ack(channel, mp.getDeliveryTag());
    }
}
