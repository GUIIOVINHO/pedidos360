package cl.duoc.pedidos360.notificaciones.consumer;

import cl.duoc.pedidos360.notificaciones.dto.MetricasPedidosDto;
import cl.duoc.pedidos360.notificaciones.exception.EventoInvalidoException;
import cl.duoc.pedidos360.notificaciones.service.AlertaDemandaService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * CONSUMIDOR Kafka del topico pedidos.metricas (grupo notificaciones-alertas).
 * Es un grupo distinto al de analytics-service, por eso recibe todas las fotos de metricas.
 * Solo adapta el mensaje; la decision de alertar esta en AlertaDemandaService.
 */
@Component
public class MetricasAlertaConsumer {

    private final AlertaDemandaService alertaDemandaService;

    public MetricasAlertaConsumer(AlertaDemandaService alertaDemandaService) {
        this.alertaDemandaService = alertaDemandaService;
    }

    @KafkaListener(topics = "${app.kafka.topics.metricas.name}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMetricas(ConsumerRecord<String, MetricasPedidosDto> record) {
        if (record.value() == null) {
            throw new EventoInvalidoException("Mensaje de metricas sin contenido (offset " + record.offset() + ")");
        }
        alertaDemandaService.evaluar(record.value());
    }
}
