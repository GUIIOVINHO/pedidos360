package cl.duoc.pedidos360.tickets.messaging;

import cl.duoc.pedidos360.tickets.config.RabbitNamesProperties;
import cl.duoc.pedidos360.tickets.exception.NonRecoverableMessageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Manejo CENTRALIZADO de confirmacion (ACK) y errores para todos los consumidores del servicio.
 * El contenedor esta en modo ACK manual, asi que cada mensaje termina SIEMPRE en una de estas rutas:
 *
 *  1. OK .......................... basicAck               -> el mensaje sale de la cola.
 *  2. Mensaje invalido o error no
 *     recuperable ................. basicNack(requeue=false) -> va al Dead Letter Exchange -> DLQ (sin reintentos).
 *  3. Error recuperable (transitorio)
 *     y quedan reintentos ......... se re-publica el mensaje en la misma cola con retry-count+1
 *                                   (tras una pausa) y se hace ACK del original.
 *  4. Error recuperable sin
 *     reintentos disponibles ...... basicNack(requeue=false) -> DLQ.
 *
 * Nunca se usa requeue=true para evitar bucles infinitos de reprocesamiento.
 */
@Component
public class MessageAckSupport {

    private static final Logger log = LoggerFactory.getLogger(MessageAckSupport.class);

    /** Cabecera con el numero de reintentos que lleva el mensaje. */
    public static final String RETRY_HEADER = "retry-count";

    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitNamesProperties props;

    public MessageAckSupport(ObjectMapper objectMapper, RabbitTemplate rabbitTemplate, RabbitNamesProperties props) {
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public <T> void handle(Message message, Channel channel, Class<T> type, MessageProcessor<T> processor) {
        MessageProperties mp = message.getMessageProperties();
        long tag = mp.getDeliveryTag();
        String queue = mp.getConsumerQueue();
        int retries = currentRetries(mp);

        // 1) Deserializacion: si el JSON es invalido no tiene sentido reintentar -> DLQ.
        T payload;
        try {
            payload = objectMapper.readValue(message.getBody(), type);
        } catch (IOException ex) {
            log.error("[{}] Mensaje con formato invalido, se envia a la DLQ. Causa: {}", queue, ex.getMessage());
            reject(channel, tag);
            return;
        }
        if (payload == null) {
            log.error("[{}] Mensaje vacio, se envia a la DLQ.", queue);
            reject(channel, tag);
            return;
        }

        // 2) Procesamiento de negocio y decision de ACK / reintento / DLQ.
        try {
            processor.process(payload, retries + 1);
            ack(channel, tag);
        } catch (NonRecoverableMessageException ex) {
            log.error("[{}] Error NO recuperable: {} -> DLQ (sin reintentos).", queue, ex.getMessage());
            reject(channel, tag);
        } catch (Exception ex) {
            if (retries < props.retry().maxRetries()) {
                log.warn("[{}] Error recuperable: {} -> reintento {}/{}", queue, ex.getMessage(),
                        retries + 1, props.retry().maxRetries());
                retry(message, channel, tag, queue, retries + 1);
            } else {
                log.error("[{}] Error recuperable pero se agotaron los {} reintentos: {} -> DLQ.",
                        queue, props.retry().maxRetries(), ex.getMessage());
                reject(channel, tag);
            }
        }
    }

    private void retry(Message message, Channel channel, long tag, String queue, int nextRetry) {
        try {
            Thread.sleep(props.retry().backoffMs());
            Message copy = MessageBuilder.fromMessage(message).setHeader(RETRY_HEADER, nextRetry).build();
            // Exchange por defecto ("") + nombre de la cola: se re-entrega SOLO a esta cola.
            rabbitTemplate.send("", queue, copy);
            ack(channel, tag);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            reject(channel, tag);
        } catch (AmqpException ex) {
            log.error("[{}] No se pudo re-publicar el mensaje para reintento ({}) -> DLQ.", queue, ex.getMessage());
            reject(channel, tag);
        }
    }

    private int currentRetries(MessageProperties mp) {
        Object header = mp.getHeader(RETRY_HEADER);
        return header instanceof Number number ? number.intValue() : 0;
    }

    public void ack(Channel channel, long tag) {
        try {
            channel.basicAck(tag, false);
        } catch (IOException ex) {
            log.error("No se pudo confirmar (ACK) el mensaje {}: {}", tag, ex.getMessage());
        }
    }

    public void reject(Channel channel, long tag) {
        try {
            // requeue=false: RabbitMQ lo enruta al dead letter exchange de la cola.
            channel.basicNack(tag, false, false);
        } catch (IOException ex) {
            log.error("No se pudo rechazar (NACK) el mensaje {}: {}", tag, ex.getMessage());
        }
    }
}
