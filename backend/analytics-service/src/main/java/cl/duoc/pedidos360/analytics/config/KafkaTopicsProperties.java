package cl.duoc.pedidos360.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Topicos y politica de reintentos de este microservicio, leidos desde application.yml (prefijo app.kafka). */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaTopicsProperties(Topics topics, Retry retry) {

    public record Topics(TopicDefinition eventos, TopicDefinition metricas, TopicDefinition dlt) {
    }

    public record TopicDefinition(String name, int partitions, int replicas, long retentionMs,
                                  String cleanupPolicy, int minInsyncReplicas) {
    }

    public record Retry(int maxRetries, long backoffMs) {
    }
}
