package cl.duoc.pedidos360.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Definicion de los topicos de Kafka que gestiona este microservicio, leida desde application.yml
 * (prefijo app.kafka). Es la UNICA fuente de nombres y parametros de topicos del servicio.
 */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaTopicsProperties(Topics topics) {

    public record Topics(TopicDefinition eventos) {
    }

    public record TopicDefinition(String name, int partitions, int replicas, long retentionMs,
                                  String cleanupPolicy, int minInsyncReplicas) {
    }
}
