package cl.duoc.pedidos360.analytics.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Topicos de los que es dueño este servicio:
 *  - pedidos.metricas: cleanup.policy=compact, conserva solo la ultima foto de las metricas por clave.
 *  - DLT: guarda los eventos que no se pudieron procesar (retencion larga para poder analizarlos).
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class KafkaTopicConfig {

    @Bean
    public NewTopic pedidosMetricasTopic(KafkaTopicsProperties props) {
        return construir(props.topics().metricas());
    }

    @Bean
    public NewTopic pedidosEventosAnalyticsDlt(KafkaTopicsProperties props) {
        return construir(props.topics().dlt());
    }

    private NewTopic construir(KafkaTopicsProperties.TopicDefinition t) {
        return TopicBuilder.name(t.name())
                .partitions(t.partitions())
                .replicas(t.replicas())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(t.retentionMs()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, t.cleanupPolicy())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(t.minInsyncReplicas()))
                .build();
    }
}
