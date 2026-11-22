package cl.duoc.pedidos360.kafkaadmin;

import cl.duoc.pedidos360.kafkaadmin.dto.CleanupPolicy;
import cl.duoc.pedidos360.kafkaadmin.dto.TopicRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopicRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private TopicRequest valido() {
        TopicRequest request = new TopicRequest();
        request.setName("pedidos.prueba");
        request.setPartitions(3);
        request.setReplicationFactor(2);
        request.setRetentionMs(86_400_000L);
        request.setCleanupPolicy(CleanupPolicy.DELETE);
        request.setMinInsyncReplicas(1);
        return request;
    }

    @Test
    void unaConfiguracionValidaNoTieneErrores() {
        assertTrue(validator.validate(valido()).isEmpty());
    }

    @Test
    void nombreVacioEsInvalido() {
        TopicRequest request = valido();
        request.setName("");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void nombreConEspaciosEsInvalido() {
        TopicRequest request = valido();
        request.setName("mi topico");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void ceroParticionesEsInvalido() {
        TopicRequest request = valido();
        request.setPartitions(0);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void minInsyncMayorAlFactorDeReplicacionEsInvalido() {
        TopicRequest request = valido();
        request.setMinInsyncReplicas(5);
        assertFalse(validator.validate(request).isEmpty());
    }
}
