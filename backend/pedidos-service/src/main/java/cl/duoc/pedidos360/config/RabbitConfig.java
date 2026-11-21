package cl.duoc.pedidos360.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de mensajeria del productor (unica clase con beans de RabbitMQ en este servicio).
 * El servicio de pedidos solo publica en el exchange; las colas y bindings los declaran
 * los consumidores (notificaciones-service y tickets-service).
 */
@Configuration
@EnableConfigurationProperties(RabbitNamesProperties.class)
public class RabbitConfig {

    /** Exchange topic donde se publican todos los eventos de pedidos. */
    @Bean
    public TopicExchange pedidosExchange(RabbitNamesProperties props) {
        return new TopicExchange(props.exchanges().pedidos(), true, false);
    }

    /** Los eventos viajan como JSON. */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
