package cl.duoc.pedidos360.analytics.messaging;

import cl.duoc.pedidos360.analytics.config.KafkaTopicsProperties;
import cl.duoc.pedidos360.analytics.dto.MetricasPedidosDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * PRODUCTOR Kafka: publica la foto de metricas en el topico pedidos.metricas (compactado) con la clave "global".
 * Gracias a la compactacion, el topico conserva siempre la ultima foto vigente.
 */
@Component
public class MetricasPublisher {

    private static final Logger log = LoggerFactory.getLogger(MetricasPublisher.class);
    private static final String CLAVE = "global";

    private final KafkaTemplate<String, MetricasPedidosDto> kafkaTemplate;
    private final KafkaTopicsProperties props;

    public MetricasPublisher(KafkaTemplate<String, MetricasPedidosDto> kafkaTemplate, KafkaTopicsProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    public void publicar(MetricasPedidosDto snapshot) {
        String topic = props.topics().metricas().name();
        try {
            kafkaTemplate.send(topic, CLAVE, snapshot).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[KAFKA] No se pudo publicar las metricas en '{}': {}", topic, ex.getMessage());
                } else {
                    log.info("[KAFKA] Metricas publicadas en '{}' (particion={}, offset={}): {} pedidos, {} en el ultimo minuto",
                            topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset(),
                            snapshot.totalPedidos(), snapshot.pedidosUltimoMinuto());
                }
            });
        } catch (Exception ex) {
            log.error("[KAFKA] Error al publicar las metricas: {}", ex.getMessage());
        }
    }
}
