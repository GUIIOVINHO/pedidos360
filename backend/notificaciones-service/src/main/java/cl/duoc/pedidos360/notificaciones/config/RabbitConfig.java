package cl.duoc.pedidos360.notificaciones.config;

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
 * Declara los beans Queue / Exchange / Binding del caso de uso "notificaciones":
 *
 *   pedidos.exchange (topic) --pedido.creado-------\
 *                            --pedido.actualizado---+--> notificaciones.queue --(rechazo / reintentos agotados)--> pedidos.dlx --> notificaciones.dlq
 *
 * La logica de negocio (NotificacionService) y los consumidores no declaran nada de esto.
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

    // ---------- Caso de uso: notificaciones ----------

    @Bean
    public Queue notificacionesQueue(RabbitNamesProperties props) {
        return QueueBuilder.durable(props.queues().notificaciones())
                .deadLetterExchange(props.exchanges().dlx())
                .deadLetterRoutingKey(props.routingKeys().notificacionesDead())
                .build();
    }

    @Bean
    public Queue notificacionesDlq(RabbitNamesProperties props) {
        return QueueBuilder.durable(props.queues().notificacionesDlq()).build();
    }

    @Bean
    public Binding notificacionesPedidoCreadoBinding(RabbitNamesProperties props) {
        return BindingBuilder.bind(notificacionesQueue(props))
                .to(pedidosExchange(props))
                .with(props.routingKeys().pedidoCreado());
    }

    @Bean
    public Binding notificacionesPedidoActualizadoBinding(RabbitNamesProperties props) {
        return BindingBuilder.bind(notificacionesQueue(props))
                .to(pedidosExchange(props))
                .with(props.routingKeys().pedidoActualizado());
    }

    @Bean
    public Binding notificacionesDlqBinding(RabbitNamesProperties props) {
        return BindingBuilder.bind(notificacionesDlq(props))
                .to(deadLetterExchange(props))
                .with(props.routingKeys().notificacionesDead());
    }

    // ---------- Conversion de mensajes ----------
    // A proposito NO se declara un MessageConverter JSON en los consumidores.
    // El mensaje llega con la cabecera __TypeId__ del PRODUCTOR (cl.duoc.pedidos360.event.PedidoEvent),
    // una clase que no existe en este microservicio; un conversor JSON del contenedor intentaria
    // cargarla y rechazaria todos los mensajes. Por eso el listener recibe el mensaje crudo y
    // MessageAckSupport lo deserializa con ObjectMapper hacia el PedidoEvent propio de este servicio.
}
