package cl.duoc.pedidos360.rabbitadmin;

import cl.duoc.pedidos360.rabbitadmin.dto.QueueRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.QueueResponse;
import cl.duoc.pedidos360.rabbitadmin.exception.RecursoNoEncontradoException;
import cl.duoc.pedidos360.rabbitadmin.exception.RecursoYaExisteException;
import cl.duoc.pedidos360.rabbitadmin.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RabbitAdminServiceTest {

    private final AmqpAdmin amqpAdmin = mock(AmqpAdmin.class);
    private final RabbitAdminService service = new RabbitAdminService(amqpAdmin);

    private QueueRequest request(String nombre) {
        QueueRequest request = new QueueRequest();
        request.setName(nombre);
        return request;
    }

    @Test
    void creaUnaColaNueva() {
        QueueInformation info = new QueueInformation("cola.test", 0, 0);
        when(amqpAdmin.getQueueInfo("cola.test")).thenReturn(null, info);

        QueueResponse respuesta = service.crearCola(request("cola.test"));

        verify(amqpAdmin).declareQueue(any(Queue.class));
        assertEquals("cola.test", respuesta.name());
    }

    @Test
    void noCreaUnaColaQueYaExiste() {
        when(amqpAdmin.getQueueInfo("cola.test")).thenReturn(new QueueInformation("cola.test", 0, 0));

        assertThrows(RecursoYaExisteException.class, () -> service.crearCola(request("cola.test")));
        verify(amqpAdmin, never()).declareQueue(any(Queue.class));
    }

    @Test
    void eliminarUnaColaInexistenteDevuelveNoEncontrado() {
        when(amqpAdmin.getQueueInfo("no.existe")).thenReturn(null);

        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminarCola("no.existe"));
    }

    @Test
    void eliminaUnaColaExistente() {
        when(amqpAdmin.getQueueInfo("cola.test")).thenReturn(new QueueInformation("cola.test", 0, 0));

        service.eliminarCola("cola.test");

        verify(amqpAdmin).deleteQueue("cola.test");
    }

    @Test
    void rechazaNombresReservados() {
        assertThrows(IllegalArgumentException.class, () -> service.eliminarCola("amq.gen-123"));
    }
}
