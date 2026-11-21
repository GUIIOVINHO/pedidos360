package cl.duoc.pedidos360.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de exchanges y routing keys de RabbitMQ, leidos desde application.yml
 * (prefijo app.rabbit). Es la UNICA fuente de nombres para este microservicio.
 */
@ConfigurationProperties(prefix = "app.rabbit")
public record RabbitNamesProperties(Exchanges exchanges, RoutingKeys routingKeys) {

    public record Exchanges(String pedidos) {
    }

    public record RoutingKeys(String pedidoCreado, String pedidoActualizado) {
    }
}
