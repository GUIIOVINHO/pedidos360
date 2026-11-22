package cl.duoc.pedidos360.notificaciones.config;

import cl.duoc.pedidos360.notificaciones.exception.EventoInvalidoException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * UNICA clase con la configuracion Kafka de este microservicio (el RabbitConfig de RabbitMQ no se toca):
 * declara su topico DLT y la estrategia de errores del consumidor (reintentos + DLT + logs).
 * Es la misma estrategia que en analytics-service:
 *   malformado / invalido -> DLT directa;  error transitorio -> reintentos y luego DLT.
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public NewTopic metricasNotificacionesDlt(KafkaTopicsProperties props) {
        KafkaTopicsProperties.TopicDefinition t = props.topics().dlt();
        return TopicBuilder.name(t.name())
                .partitions(t.partitions())
                .replicas(t.replicas())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(t.retentionMs()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, t.cleanupPolicy())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(t.minInsyncReplicas()))
                .build();
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
                                                KafkaTopicsProperties props) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);

        Map<Class<?>, Serializer<?>> serializers = new LinkedHashMap<>();
        serializers.put(byte[].class, new ByteArraySerializer());
        serializers.put(Object.class, new JsonSerializer<Object>());

        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(
                config, new StringSerializer(), new DelegatingByTypeSerializer(serializers, true));
        KafkaTemplate<String, Object> dltTemplate = new KafkaTemplate<>(factory);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltTemplate,
                (record, ex) -> new TopicPartition(props.topics().dlt().name(), -1));

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(props.retry().backoffMs(), props.retry().maxRetries()));
        handler.addNotRetryableExceptions(EventoInvalidoException.class);
        // El "Seek to current after exception" es el mecanismo normal de reintento de Spring Kafka:
        // se registra en DEBUG para no llenar el log de trazas rojas (los reintentos ya se informan en WARN).
        handler.setLogLevel(KafkaException.Level.DEBUG);
        handler.setRetryListeners(new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
                log.warn("[KAFKA] Error procesando {}-{}@{} (intento {}): {}",
                        record.topic(), record.partition(), record.offset(), deliveryAttempt, causaRaiz(ex));
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
                log.error("[KAFKA] Registro {}-{}@{} enviado a la DLT '{}': {}",
                        record.topic(), record.partition(), record.offset(), props.topics().dlt().name(), causaRaiz(ex));
            }
        });
        return handler;
    }

    private static String causaRaiz(Throwable ex) {
        Throwable actual = ex;
        while (actual.getCause() != null && actual.getCause() != actual) {
            actual = actual.getCause();
        }
        return actual.getMessage();
    }
}
