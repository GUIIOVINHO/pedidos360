package cl.duoc.pedidos360.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declaracion del topico de eventos de pedidos. Spring (KafkaAdmin) lo crea al arrancar si no existe
 * y, si ya existe, ajusta sus configuraciones a las definidas en application.yml.
 *
 * Particiones: permiten paralelismo (un consumidor por particion dentro de un grupo).
 * Replicas: tolerancia a la caida de un broker.
 * Clave del mensaje = id del pedido, asi todos los eventos de un pedido caen en la misma particion y se mantienen en orden.
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class KafkaTopicConfig {

    @Bean
    public NewTopic pedidosEventosTopic(KafkaTopicsProperties props) {
        KafkaTopicsProperties.TopicDefinition t = props.topics().eventos();
        return TopicBuilder.name(t.name())
                .partitions(t.partitions())
                .replicas(t.replicas())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(t.retentionMs()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, t.cleanupPolicy())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(t.minInsyncReplicas()))
                .build();
    }
}
