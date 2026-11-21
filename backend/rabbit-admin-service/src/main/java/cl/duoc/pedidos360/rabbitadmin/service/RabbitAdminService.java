package cl.duoc.pedidos360.rabbitadmin.service;

import cl.duoc.pedidos360.rabbitadmin.dto.BindingRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.BindingResponse;
import cl.duoc.pedidos360.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.ExchangeResponse;
import cl.duoc.pedidos360.rabbitadmin.dto.QueueRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.QueueResponse;
import cl.duoc.pedidos360.rabbitadmin.dto.ValidationPatterns;
import cl.duoc.pedidos360.rabbitadmin.exception.RecursoNoEncontradoException;
import cl.duoc.pedidos360.rabbitadmin.exception.RecursoYaExisteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * UNICO punto del microservicio que habla con la libreria de RabbitMQ (AmqpAdmin).
 * Encapsula la creacion, consulta y eliminacion de colas, exchanges y bindings;
 * los controladores solo llaman a estos metodos de alto nivel.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);
    private static final Pattern NOMBRE_CREABLE = Pattern.compile(ValidationPatterns.NOMBRE_CREABLE);

    private final AmqpAdmin amqpAdmin;

    public RabbitAdminService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    // ---------------------------- Colas ----------------------------

    public QueueResponse crearCola(QueueRequest request) {
        if (amqpAdmin.getQueueInfo(request.getName()) != null) {
            throw new RecursoYaExisteException("La cola '" + request.getName() + "' ya existe");
        }
        Map<String, Object> args = new HashMap<>();
        if (tieneTexto(request.getDeadLetterExchange())) {
            args.put("x-dead-letter-exchange", request.getDeadLetterExchange());
        }
        if (tieneTexto(request.getDeadLetterRoutingKey())) {
            args.put("x-dead-letter-routing-key", request.getDeadLetterRoutingKey());
        }
        if (request.getMessageTtlMs() != null) {
            args.put("x-message-ttl", request.getMessageTtlMs());
        }
        Queue queue = new Queue(request.getName(), request.isDurable(), false, request.isAutoDelete(), args);
        amqpAdmin.declareQueue(queue);
        log.info("Cola creada: {}", request.getName());
        return obtenerCola(request.getName());
    }

    public QueueResponse obtenerCola(String nombre) {
        validarNombre(nombre);
        QueueInformation info = amqpAdmin.getQueueInfo(nombre);
        if (info == null) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        return new QueueResponse(info.getName(), info.getMessageCount(), info.getConsumerCount());
    }

    public void eliminarCola(String nombre) {
        validarNombre(nombre);
        if (amqpAdmin.getQueueInfo(nombre) == null) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        amqpAdmin.deleteQueue(nombre);
        log.info("Cola eliminada: {}", nombre);
    }

    // ---------------------------- Exchanges ----------------------------

    public ExchangeResponse crearExchange(ExchangeRequest request) {
        String nombre = request.getName();
        boolean durable = request.isDurable();
        boolean autoDelete = request.isAutoDelete();
        Exchange exchange = switch (request.getType()) {
            case DIRECT -> new DirectExchange(nombre, durable, autoDelete);
            case TOPIC -> new TopicExchange(nombre, durable, autoDelete);
            case FANOUT -> new FanoutExchange(nombre, durable, autoDelete);
            case HEADERS -> new HeadersExchange(nombre, durable, autoDelete);
        };
        amqpAdmin.declareExchange(exchange);
        log.info("Exchange creado: {} ({})", nombre, request.getType());
        return new ExchangeResponse(nombre, request.getType(), durable, autoDelete);
    }

    public void eliminarExchange(String nombre) {
        validarNombre(nombre);
        amqpAdmin.deleteExchange(nombre);
        log.info("Exchange eliminado: {}", nombre);
    }

    // ---------------------------- Bindings ----------------------------

    public BindingResponse crearBinding(BindingRequest request) {
        asegurarQueExisteLaCola(request.getQueue());
        amqpAdmin.declareBinding(aBinding(request));
        log.info("Binding creado: {} <- {} [{}]", request.getQueue(), request.getExchange(), request.getRoutingKey());
        return new BindingResponse(request.getQueue(), request.getExchange(), request.getRoutingKey());
    }

    public void eliminarBinding(BindingRequest request) {
        asegurarQueExisteLaCola(request.getQueue());
        amqpAdmin.removeBinding(aBinding(request));
        log.info("Binding eliminado: {} <- {} [{}]", request.getQueue(), request.getExchange(), request.getRoutingKey());
    }

    // ---------------------------- Utilidades ----------------------------

    private Binding aBinding(BindingRequest request) {
        return new Binding(request.getQueue(), Binding.DestinationType.QUEUE,
                request.getExchange(), request.getRoutingKey(), null);
    }

    private void asegurarQueExisteLaCola(String nombre) {
        if (amqpAdmin.getQueueInfo(nombre) == null) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
    }

    /** Valida nombres recibidos por URL (las peticiones con cuerpo se validan con anotaciones en los DTO). */
    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.length() > ValidationPatterns.MAX_LONGITUD
                || !NOMBRE_CREABLE.matcher(nombre).matches()) {
            throw new IllegalArgumentException(
                    "Nombre invalido: solo letras, numeros, '.', '_' y '-', maximo 255 caracteres y sin el prefijo reservado 'amq.'");
        }
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
