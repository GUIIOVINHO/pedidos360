package cl.duoc.pedidos360.rabbitadmin;

import cl.duoc.pedidos360.rabbitadmin.dto.QueueRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueueRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private QueueRequest request(String nombre) {
        QueueRequest request = new QueueRequest();
        request.setName(nombre);
        return request;
    }

    @Test
    void nombreVacioEsInvalido() {
        assertFalse(validator.validate(request("")).isEmpty());
    }

    @Test
    void nombreConEspaciosEsInvalido() {
        assertFalse(validator.validate(request("mi cola")).isEmpty());
    }

    @Test
    void nombreReservadoEsInvalido() {
        assertFalse(validator.validate(request("amq.reservada")).isEmpty());
    }

    @Test
    void ttlNegativoEsInvalido() {
        QueueRequest request = request("cola.ttl");
        request.setMessageTtlMs(-5L);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void deadLetterRoutingKeySinExchangeEsInvalido() {
        QueueRequest request = request("cola.dlx");
        request.setDeadLetterRoutingKey("clave");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void configuracionValidaNoTieneErrores() {
        QueueRequest request = request("cola.valida");
        request.setDeadLetterExchange("pedidos.dlx");
        request.setDeadLetterRoutingKey("clave");
        request.setMessageTtlMs(60000L);
        assertTrue(validator.validate(request).isEmpty());
    }
}
