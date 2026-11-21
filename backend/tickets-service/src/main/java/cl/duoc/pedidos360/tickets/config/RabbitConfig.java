package cl.duoc.pedidos360.tickets.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * UNICA clase con la configuracion de mensajeria de este microservicio.
 * Declara los beans Queue / Exchange / Binding del caso de uso "generacion de tickets":
 *
 *   pedidos.exchange (topic) --pedido.creado--> tickets.queue --(rechazo / reintentos agotados)--> pedidos.dlx --> tickets.dlq
 *
 * La logica de negocio (TicketService) y los consumidores no declaran nada de esto.
 */
@Configuration
@EnableConfigurationProperties(RabbitNamesProperties.class)
public class RabbitConfig {

    // ---------- Exchanges ----------

    /** Exchange principal (el mismo que declara el productor; la declaracion es idempotente). */
    @Bean
    public TopicExchange pedidosExchange(RabbitNamesProperties props) {
        return new TopicExchange(props.exchanges().pedidos(), true, false);
    }

    /** Dead Letter Exchange: recibe los mensajes rechazados (NACK sin requeue). */
    @Bean
    public DirectExchange deadLetterExchange(RabbitNamesProperties props) {
        return new DirectExchange(props.exchanges().dlx(), true, false);
    }

    // ---------- Caso de uso: generacion de tickets ----------

    @Bean
    public Queue ticketsQueue(RabbitNamesProperties props) {
        return QueueBuilder.durable(props.queues().tickets())
                .deadLetterExchange(props.exchanges().dlx())
                .deadLetterRoutingKey(props.routingKeys().ticketsDead())
                .build();
    }

    @Bean
    public Queue ticketsDlq(RabbitNamesProperties props) {
        return QueueBuilder.durable(props.queues().ticketsDlq()).build();
    }

    @Bean
    public Binding ticketsPedidoCreadoBinding(RabbitNamesProperties props) {
        return BindingBuilder.bind(ticketsQueue(props))
                .to(pedidosExchange(props))
                .with(props.routingKeys().pedidoCreado());
    }

    @Bean
    public Binding ticketsDlqBinding(RabbitNamesProperties props) {
        return BindingBuilder.bind(ticketsDlq(props))
                .to(deadLetterExchange(props))
                .with(props.routingKeys().ticketsDead());
    }

    // ---------- Conversion de mensajes ----------
    // A proposito NO se declara un MessageConverter JSON en los consumidores.
    // El mensaje llega con la cabecera __TypeId__ del PRODUCTOR (cl.duoc.pedidos360.event.PedidoEvent),
    // una clase que no existe en este microservicio; un conversor JSON del contenedor intentaria
    // cargarla y rechazaria todos los mensajes. Por eso el listener recibe el mensaje crudo y
    // MessageAckSupport lo deserializa con ObjectMapper hacia el PedidoEvent propio de este servicio.
}
