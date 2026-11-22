package cl.duoc.pedidos360.notificaciones.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Topicos y politica de reintentos Kafka de este microservicio (prefijo app.kafka en application.yml). */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaTopicsProperties(Topics topics, Retry retry) {

    public record Topics(TopicDefinition metricas, TopicDefinition dlt) {
    }

    public record TopicDefinition(String name, int partitions, int replicas, long retentionMs,
                                  String cleanupPolicy, int minInsyncReplicas) {
    }

    public record Retry(int maxRetries, long backoffMs) {
    }
}
