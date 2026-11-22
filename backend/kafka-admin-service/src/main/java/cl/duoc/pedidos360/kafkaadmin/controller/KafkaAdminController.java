package cl.duoc.pedidos360.kafkaadmin.controller;

import cl.duoc.pedidos360.kafkaadmin.dto.ClusterInfo;
import cl.duoc.pedidos360.kafkaadmin.dto.ConsumerGroupDetail;
import cl.duoc.pedidos360.kafkaadmin.dto.ConsumerGroupSummary;
import cl.duoc.pedidos360.kafkaadmin.dto.PartitionsRequest;
import cl.duoc.pedidos360.kafkaadmin.dto.TopicConfigUpdateRequest;
import cl.duoc.pedidos360.kafkaadmin.dto.TopicDetail;
import cl.duoc.pedidos360.kafkaadmin.dto.TopicRequest;
import cl.duoc.pedidos360.kafkaadmin.dto.TopicSummary;
import cl.duoc.pedidos360.kafkaadmin.service.KafkaAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * API REST de administracion de Kafka. Todas las rutas estan protegidas por FirebaseAuthFilter (JWT).
 *
 *   GET    /api/cluster                        brokers y controlador
 *   GET    /api/topics                         lista los topicos
 *   POST   /api/topics                         crea un topico (particiones, replicas, retencion, cleanup policy...)  (201)
 *   GET    /api/topics/{name}                  describe un topico: particiones, lideres, ISR y configuraciones
 *   PATCH  /api/topics/{name}/config           modifica o elimina configuraciones del topico
 *   PUT    /api/topics/{name}/partitions       aumenta el numero de particiones
 *   DELETE /api/topics/{name}                  elimina el topico                                                     (204)
 *   GET    /api/consumer-groups                lista los consumer groups
 *   GET    /api/consumer-groups/{groupId}      miembros, particiones asignadas y lag de un grupo
 */
@RestController
@RequestMapping("/api")
public class KafkaAdminController {

    private final KafkaAdminService kafkaAdminService;

    public KafkaAdminController(KafkaAdminService kafkaAdminService) {
        this.kafkaAdminService = kafkaAdminService;
    }

    @GetMapping("/cluster")
    public ClusterInfo cluster() {
        return kafkaAdminService.cluster();
    }

    @GetMapping("/topics")
    public List<TopicSummary> listarTopics() {
        return kafkaAdminService.listarTopics();
    }

    @PostMapping("/topics")
    public ResponseEntity<TopicDetail> crearTopic(@Valid @RequestBody TopicRequest request) {
        TopicDetail creado = kafkaAdminService.crearTopic(request);
        return ResponseEntity.created(URI.create("/api/topics/" + creado.name())).body(creado);
    }

    @GetMapping("/topics/{name}")
    public TopicDetail describirTopic(@PathVariable String name) {
        return kafkaAdminService.describirTopic(name);
    }

    @PatchMapping("/topics/{name}/config")
    public TopicDetail actualizarConfiguracion(@PathVariable String name,
                                               @Valid @RequestBody TopicConfigUpdateRequest request) {
        return kafkaAdminService.actualizarConfiguracion(name, request);
    }

    @PutMapping("/topics/{name}/partitions")
    public TopicDetail aumentarParticiones(@PathVariable String name,
                                           @Valid @RequestBody PartitionsRequest request) {
        return kafkaAdminService.aumentarParticiones(name, request.getPartitions());
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<Void> eliminarTopic(@PathVariable String name) {
        kafkaAdminService.eliminarTopic(name);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/consumer-groups")
    public List<ConsumerGroupSummary> listarGrupos() {
        return kafkaAdminService.listarGrupos();
    }

    @GetMapping("/consumer-groups/{groupId}")
    public ConsumerGroupDetail describirGrupo(@PathVariable String groupId) {
        return kafkaAdminService.describirGrupo(groupId);
    }
}
