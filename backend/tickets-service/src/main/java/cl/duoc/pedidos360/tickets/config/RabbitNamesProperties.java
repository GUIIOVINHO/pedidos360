package cl.duoc.pedidos360.tickets.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de exchanges, colas y routing keys de RabbitMQ leidos desde application.yml
 * (prefijo app.rabbit). Es la UNICA fuente de nombres de este microservicio.
 */
@ConfigurationProperties(prefix = "app.rabbit")
public record RabbitNamesProperties(Exchanges exchanges, Queues queues, RoutingKeys routingKeys, Retry retry) {

    public record Exchanges(String pedidos, String dlx) {
    }

    public record Queues(String tickets, String ticketsDlq) {
    }

    public record RoutingKeys(String pedidoCreado, String ticketsDead) {
    }

    public record Retry(int maxRetries, long backoffMs) {
    }
}
