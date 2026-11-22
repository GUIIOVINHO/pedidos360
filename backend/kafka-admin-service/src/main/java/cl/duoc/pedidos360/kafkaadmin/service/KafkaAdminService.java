package cl.duoc.pedidos360.kafkaadmin.service;

import cl.duoc.pedidos360.kafkaadmin.dto.BrokerInfo;
import cl.duoc.pedidos360.kafkaadmin.dto.ClusterInfo;
import cl.duoc.pedidos360.kafkaadmin.dto.ConsumerGroupDetail;
import cl.duoc.pedidos360.kafkaadmin.dto.ConsumerGroupSummary;
import cl.duoc.pedidos360.kafkaadmin.dto.GroupMemberInfo;
import cl.duoc.pedidos360.kafkaadmin.dto.GroupPartitionLag;
import cl.duoc.pedidos360.kafkaadmin.dto.PartitionInfo;
import cl.duoc.pedidos360.kafkaadmin.dto.TopicConfigUpdateRequest;
import cl.duoc.pedidos360.kafkaadmin.dto.TopicDetail;
import cl.duoc.pedidos360.kafkaadmin.dto.TopicRequest;
import cl.duoc.pedidos360.kafkaadmin.dto.TopicSummary;
import cl.duoc.pedidos360.kafkaadmin.exception.KafkaNoDisponibleException;
import cl.duoc.pedidos360.kafkaadmin.exception.OperacionInvalidaException;
import cl.duoc.pedidos360.kafkaadmin.exception.RecursoNoEncontradoException;
import cl.duoc.pedidos360.kafkaadmin.exception.RecursoYaExisteException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * UNICO punto del microservicio que habla con Kafka (AdminClient). Encapsula la administracion de
 * topicos (crear, listar, describir, modificar configuraciones, aumentar particiones, eliminar),
 * del cluster y de los consumer groups (listar, describir y calcular el lag).
 * Los controladores solo llaman a estos metodos de alto nivel.
 */
@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[a-zA-Z0-9._-]{1,249}$");

    /** Configuraciones de topico que se permite asignar desde la API. */
    static final Set<String> CONFIGS_PERMITIDAS = Set.of(
            "retention.ms", "retention.bytes", "cleanup.policy", "min.insync.replicas",
            "max.message.bytes", "segment.ms", "segment.bytes", "compression.type",
            "delete.retention.ms", "min.cleanable.dirty.ratio");

    private static final Set<String> POLITICAS_VALIDAS = Set.of("delete", "compact", "compact,delete", "delete,compact");

    private final Admin admin;
    private final long timeoutSegundos;

    public KafkaAdminService(Admin admin, @Value("${app.kafka-admin.timeout-seconds:10}") long timeoutSegundos) {
        this.admin = admin;
        this.timeoutSegundos = timeoutSegundos;
    }

    // ------------------------------- Cluster -------------------------------

    public ClusterInfo cluster() {
        DescribeClusterResult resultado = admin.describeCluster();
        String clusterId = esperar(resultado.clusterId());
        Node controlador = esperar(resultado.controller());
        Collection<Node> nodos = esperar(resultado.nodes());
        List<BrokerInfo> brokers = nodos.stream()
                .map(n -> new BrokerInfo(n.id(), n.host(), n.port()))
                .sorted(Comparator.comparingInt(BrokerInfo::id))
                .toList();
        return new ClusterInfo(clusterId, controlador == null ? null : controlador.id(), brokers);
    }

    // ------------------------------- Topicos -------------------------------

    public List<TopicSummary> listarTopics() {
        Set<String> nombres = esperar(admin.listTopics(new ListTopicsOptions().listInternal(false)).names());
        if (nombres.isEmpty()) {
            return List.of();
        }
        Map<String, TopicDescription> descripciones = esperar(admin.describeTopics(nombres).allTopicNames());
        return descripciones.values().stream()
                .map(d -> new TopicSummary(d.name(), d.partitions().size(), factorReplicacion(d)))
                .sorted(Comparator.comparing(TopicSummary::name))
                .toList();
    }

    public TopicDetail describirTopic(String nombre) {
        validarNombre(nombre);
        Map<String, TopicDescription> descripciones = esperar(admin.describeTopics(List.of(nombre)).allTopicNames());
        TopicDescription descripcion = descripciones.get(nombre);
        if (descripcion == null) {
            throw new RecursoNoEncontradoException("El topico '" + nombre + "' no existe");
        }
        List<PartitionInfo> particiones = descripcion.partitions().stream()
                .sorted(Comparator.comparingInt(TopicPartitionInfo::partition))
                .map(p -> new PartitionInfo(p.partition(),
                        p.leader() == null ? null : p.leader().id(),
                        p.replicas().stream().map(Node::id).toList(),
                        p.isr().stream().map(Node::id).toList()))
                .toList();
        return new TopicDetail(nombre, factorReplicacion(descripcion), particiones, configuracionesDe(nombre));
    }

    public TopicDetail crearTopic(TopicRequest request) {
        Map<String, String> configs = new HashMap<>();
        if (request.getConfigs() != null) {
            configs.putAll(request.getConfigs());
        }
        if (request.getRetentionMs() != null) {
            configs.put("retention.ms", String.valueOf(request.getRetentionMs()));
        }
        if (request.getCleanupPolicy() != null) {
            configs.put("cleanup.policy", request.getCleanupPolicy().valor());
        }
        if (request.getMinInsyncReplicas() != null) {
            configs.put("min.insync.replicas", String.valueOf(request.getMinInsyncReplicas()));
        }
        validarConfiguraciones(configs);

        NewTopic nuevo = new NewTopic(request.getName(), request.getPartitions(),
                request.getReplicationFactor().shortValue()).configs(configs);
        esperar(admin.createTopics(List.of(nuevo)).all());
        log.info("Topico creado: {} ({} particiones, factor de replicacion {})",
                request.getName(), request.getPartitions(), request.getReplicationFactor());
        return describirTrasCrear(request.getName());
    }

    public TopicDetail actualizarConfiguracion(String nombre, TopicConfigUpdateRequest request) {
        validarNombre(nombre);
        describirTopic(nombre);   // 404 si no existe

        Map<String, String> asignar = request.getSet() == null ? Map.of() : request.getSet();
        List<String> quitar = request.getRemove() == null ? List.of() : request.getRemove();
        validarConfiguraciones(asignar);
        for (String clave : quitar) {
            if (!CONFIGS_PERMITIDAS.contains(clave)) {
                throw new OperacionInvalidaException("Configuracion no permitida: " + clave);
            }
        }

        List<AlterConfigOp> operaciones = new ArrayList<>();
        asignar.forEach((clave, valor) ->
                operaciones.add(new AlterConfigOp(new ConfigEntry(clave, valor), AlterConfigOp.OpType.SET)));
        quitar.forEach(clave ->
                operaciones.add(new AlterConfigOp(new ConfigEntry(clave, null), AlterConfigOp.OpType.DELETE)));

        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        esperar(admin.incrementalAlterConfigs(Map.of(recurso, operaciones)).all());
        log.info("Configuracion del topico {} actualizada: set={} remove={}", nombre, asignar.keySet(), quitar);
        return describirTopic(nombre);
    }

    public TopicDetail aumentarParticiones(String nombre, int nuevoTotal) {
        TopicDetail actual = describirTopic(nombre);
        if (nuevoTotal <= actual.partitions().size()) {
            throw new OperacionInvalidaException("Solo se pueden aumentar las particiones (actualmente tiene "
                    + actual.partitions().size() + ")");
        }
        esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(nuevoTotal))).all());
        log.info("Particiones del topico {} aumentadas a {}", nombre, nuevoTotal);
        return describirTopic(nombre);
    }

    public void eliminarTopic(String nombre) {
        validarNombre(nombre);
        if (nombre.startsWith("__")) {
            throw new OperacionInvalidaException("No se pueden eliminar topicos internos de Kafka");
        }
        describirTopic(nombre);   // 404 si no existe
        esperar(admin.deleteTopics(List.of(nombre)).all());
        log.info("Topico eliminado: {}", nombre);
    }

    // ------------------------------- Consumer groups -------------------------------

    public List<ConsumerGroupSummary> listarGrupos() {
        Collection<ConsumerGroupListing> grupos = esperar(admin.listConsumerGroups().all());
        return grupos.stream()
                .map(g -> new ConsumerGroupSummary(g.groupId(), g.state().map(Object::toString).orElse("UNKNOWN")))
                .sorted(Comparator.comparing(ConsumerGroupSummary::groupId))
                .toList();
    }

    public ConsumerGroupDetail describirGrupo(String groupId) {
        validarNombre(groupId);
        Map<String, ConsumerGroupDescription> descripciones = esperar(admin.describeConsumerGroups(List.of(groupId)).all());
        ConsumerGroupDescription descripcion = descripciones.get(groupId);
        if (descripcion == null || descripcion.state() == ConsumerGroupState.DEAD) {
            throw new RecursoNoEncontradoException("El consumer group '" + groupId + "' no existe");
        }

        List<GroupMemberInfo> miembros = new ArrayList<>();
        for (MemberDescription m : descripcion.members()) {
            List<String> asignadas = m.assignment().topicPartitions().stream()
                    .map(tp -> tp.topic() + "-" + tp.partition())
                    .sorted()
                    .toList();
            miembros.add(new GroupMemberInfo(m.consumerId(), m.clientId(), m.host(), asignadas));
        }

        Map<TopicPartition, OffsetAndMetadata> confirmados =
                esperar(admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());
        Map<TopicPartition, OffsetSpec> consulta = new HashMap<>();
        confirmados.keySet().forEach(tp -> consulta.put(tp, OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> finales =
                consulta.isEmpty() ? Map.of() : esperar(admin.listOffsets(consulta).all());

        List<GroupPartitionLag> particiones = new ArrayList<>();
        long lagTotal = 0;
        for (Map.Entry<TopicPartition, OffsetAndMetadata> entrada : confirmados.entrySet()) {
            if (entrada.getValue() == null || finales.get(entrada.getKey()) == null) {
                continue;
            }
            long actual = entrada.getValue().offset();
            long fin = finales.get(entrada.getKey()).offset();
            long lag = Math.max(0, fin - actual);
            lagTotal += lag;
            particiones.add(new GroupPartitionLag(entrada.getKey().topic(), entrada.getKey().partition(), actual, fin, lag));
        }
        particiones.sort(Comparator.comparing(GroupPartitionLag::topic).thenComparingInt(GroupPartitionLag::partition));

        return new ConsumerGroupDetail(groupId, descripcion.state().toString(), descripcion.partitionAssignor(),
                miembros, particiones, lagTotal);
    }

    // ------------------------------- Utilidades -------------------------------

    private Map<String, String> configuracionesDe(String nombre) {
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Map<ConfigResource, Config> resultado = esperar(admin.describeConfigs(List.of(recurso)).all());
        Config config = resultado.get(recurso);
        Map<String, String> configuraciones = new TreeMap<>();
        if (config != null) {
            for (ConfigEntry entrada : config.entries()) {
                boolean relevante = CONFIGS_PERMITIDAS.contains(entrada.name())
                        || entrada.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG;
                if (relevante && !entrada.isSensitive() && entrada.value() != null) {
                    configuraciones.put(entrada.name(), entrada.value());
                }
            }
        }
        return configuraciones;
    }

    /** Tras crear un topico, el metadato puede tardar unos milisegundos en estar disponible. */
    private TopicDetail describirTrasCrear(String nombre) {
        for (int intento = 1; intento <= 5; intento++) {
            try {
                return describirTopic(nombre);
            } catch (RecursoNoEncontradoException ex) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return describirTopic(nombre);
    }

    private int factorReplicacion(TopicDescription descripcion) {
        return descripcion.partitions().isEmpty() ? 0 : descripcion.partitions().get(0).replicas().size();
    }

    private void validarNombre(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches() || nombre.equals(".") || nombre.equals("..")) {
            throw new OperacionInvalidaException(
                    "Nombre invalido: solo letras, numeros, '.', '_' y '-', maximo 249 caracteres");
        }
    }

    private void validarConfiguraciones(Map<String, String> configuraciones) {
        for (Map.Entry<String, String> entrada : configuraciones.entrySet()) {
            if (!CONFIGS_PERMITIDAS.contains(entrada.getKey())) {
                throw new OperacionInvalidaException("Configuracion no permitida: " + entrada.getKey()
                        + ". Permitidas: " + new TreeSet<>(CONFIGS_PERMITIDAS));
            }
            if (entrada.getValue() == null || entrada.getValue().isBlank()) {
                throw new OperacionInvalidaException("La configuracion '" + entrada.getKey() + "' no tiene valor");
            }
            if (entrada.getKey().equals("cleanup.policy") && !POLITICAS_VALIDAS.contains(entrada.getValue())) {
                throw new OperacionInvalidaException("cleanup.policy debe ser delete, compact o compact,delete");
            }
        }
    }

    private <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(timeoutSegundos, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("La operacion fue interrumpida");
        } catch (TimeoutException ex) {
            throw new KafkaNoDisponibleException("Kafka no respondio a tiempo. Verifique que el cluster este levantado");
        } catch (ExecutionException ex) {
            throw traducir(ex.getCause());
        }
    }

    private RuntimeException traducir(Throwable causa) {
        if (causa instanceof UnknownTopicOrPartitionException) {
            return new RecursoNoEncontradoException("El topico no existe");
        }
        if (causa instanceof TopicExistsException) {
            return new RecursoYaExisteException("El topico ya existe");
        }
        if (causa instanceof InvalidTopicException || causa instanceof InvalidReplicationFactorException
                || causa instanceof InvalidConfigurationException || causa instanceof InvalidPartitionsException
                || causa instanceof InvalidRequestException) {
            return new OperacionInvalidaException("Kafka rechazo la operacion: " + causa.getMessage());
        }
        log.error("Error al comunicarse con Kafka: {}", causa == null ? "desconocido" : causa.getMessage());
        return new KafkaNoDisponibleException("No fue posible completar la operacion en Kafka");
    }
}
