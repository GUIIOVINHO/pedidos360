package cl.duoc.pedidos360.analytics.messaging;

import cl.duoc.pedidos360.analytics.dto.MetricasPedidosDto;
import cl.duoc.pedidos360.analytics.dto.PedidoEventoDto;
import cl.duoc.pedidos360.analytics.exception.EventoInvalidoException;
import cl.duoc.pedidos360.analytics.service.MetricasService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * CONSUMIDOR Kafka del topico pedidos.eventos (grupo analytics-metricas).
 * Solo adapta el mensaje: la logica de negocio esta en MetricasService, la publicacion en MetricasPublisher
 * y los errores los gestiona el CommonErrorHandler (reintentos + DLT) configurado en KafkaErrorHandlingConfig.
 *
 * El listener corre con concurrency=3: el broker reparte las 3 particiones entre los 3 hilos del grupo.
 */
@Component
public class PedidoEventoConsumer {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventoConsumer.class);

    private final MetricasService metricasService;
    private final MetricasPublisher metricasPublisher;

    public PedidoEventoConsumer(MetricasService metricasService, MetricasPublisher metricasPublisher) {
        this.metricasService = metricasService;
        this.metricasPublisher = metricasPublisher;
    }

    @KafkaListener(topics = "${app.kafka.topics.eventos.name}", groupId = "${spring.kafka.consumer.group-id}")
    public void onEvento(ConsumerRecord<String, PedidoEventoDto> record) {
        PedidoEventoDto evento = record.value();
        if (evento == null) {
            throw new EventoInvalidoException("Mensaje sin contenido (offset " + record.offset() + ")");
        }
        log.info("[KAFKA] Evento recibido: {} pedido #{} | particion={} offset={} clave={}",
                evento.eventType(), evento.pedidoId(), record.partition(), record.offset(), record.key());

        MetricasPedidosDto snapshot = metricasService.registrar(evento);
        metricasPublisher.publicar(snapshot);
    }
}
