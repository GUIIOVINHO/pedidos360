package cl.duoc.pedidos360.notificaciones.service;

import cl.duoc.pedidos360.notificaciones.dto.MetricasPedidosDto;
import cl.duoc.pedidos360.notificaciones.exception.EventoInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * LOGICA DE NEGOCIO de las alertas de demanda. No conoce Kafka: recibe la foto de metricas ya
 * deserializada y decide si hay que alertar (pedidos en el ultimo minuto >= umbral configurado).
 */
@Service
public class AlertaDemandaService {

    private static final Logger log = LoggerFactory.getLogger(AlertaDemandaService.class);

    private final int umbral;

    public AlertaDemandaService(@Value("${app.alertas.pedidos-por-minuto:5}") int umbral) {
        this.umbral = umbral;
    }

    /** @return true si se genero una alerta de alta demanda */
    public boolean evaluar(MetricasPedidosDto metricas) {
        if (metricas == null || metricas.pedidosUltimoMinuto() < 0) {
            throw new EventoInvalidoException("Metricas invalidas");
        }
        if (metricas.pedidosUltimoMinuto() >= umbral) {
            log.warn("[ALERTA] ALTA DEMANDA: {} pedidos en el ultimo minuto (umbral {}). Total acumulado: {} pedidos, {} unidades.",
                    metricas.pedidosUltimoMinuto(), umbral, metricas.totalPedidos(), metricas.totalUnidades());
            return true;
        }
        log.info("[METRICAS] Demanda normal: {} pedidos en el ultimo minuto (umbral {}).",
                metricas.pedidosUltimoMinuto(), umbral);
        return false;
    }
}
