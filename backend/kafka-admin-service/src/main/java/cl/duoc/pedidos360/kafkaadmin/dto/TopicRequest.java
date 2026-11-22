package cl.duoc.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

/** Datos para crear un topico. Todas las reglas se validan antes de llegar al servicio. */
public class TopicRequest {

    @NotBlank(message = "El nombre del topico es obligatorio")
    @Size(max = 249, message = "El nombre del topico no puede superar los 249 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]*$", message = "Nombre invalido: solo letras, numeros, '.', '_' y '-'")
    private String name;

    @NotNull(message = "El numero de particiones es obligatorio")
    @Min(value = 1, message = "Debe haber al menos 1 particion")
    @Max(value = 100, message = "Maximo 100 particiones")
    private Integer partitions;

    @NotNull(message = "El factor de replicacion es obligatorio")
    @Min(value = 1, message = "El factor de replicacion minimo es 1")
    @Max(value = 10, message = "El factor de replicacion maximo es 10")
    private Integer replicationFactor;

    /** Retencion en milisegundos (-1 = infinita). Opcional. */
    @Min(value = -1, message = "retentionMs debe ser -1 (infinita) o mayor a 0")
    private Long retentionMs;

    /** Opcional: delete, compact o compact_delete. */
    private CleanupPolicy cleanupPolicy;

    /** Opcional: minimo de replicas en sincronia para aceptar escrituras con acks=all. */
    @Min(value = 1, message = "minInsyncReplicas debe ser al menos 1")
    private Integer minInsyncReplicas;

    /** Opcional: otras configuraciones permitidas (ej. max.message.bytes). */
    private Map<String, String> configs;

    @JsonIgnore
    @AssertTrue(message = "minInsyncReplicas no puede ser mayor al factor de replicacion")
    public boolean isMinInsyncReplicasValido() {
        return minInsyncReplicas == null || replicationFactor == null || minInsyncReplicas <= replicationFactor;
    }

    @JsonIgnore
    @AssertTrue(message = "retentionMs no puede ser 0")
    public boolean isRetentionMsValido() {
        return retentionMs == null || retentionMs != 0;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getPartitions() { return partitions; }
    public void setPartitions(Integer partitions) { this.partitions = partitions; }

    public Integer getReplicationFactor() { return replicationFactor; }
    public void setReplicationFactor(Integer replicationFactor) { this.replicationFactor = replicationFactor; }

    public Long getRetentionMs() { return retentionMs; }
    public void setRetentionMs(Long retentionMs) { this.retentionMs = retentionMs; }

    public CleanupPolicy getCleanupPolicy() { return cleanupPolicy; }
    public void setCleanupPolicy(CleanupPolicy cleanupPolicy) { this.cleanupPolicy = cleanupPolicy; }

    public Integer getMinInsyncReplicas() { return minInsyncReplicas; }
    public void setMinInsyncReplicas(Integer minInsyncReplicas) { this.minInsyncReplicas = minInsyncReplicas; }

    public Map<String, String> getConfigs() { return configs; }
    public void setConfigs(Map<String, String> configs) { this.configs = configs; }
}
