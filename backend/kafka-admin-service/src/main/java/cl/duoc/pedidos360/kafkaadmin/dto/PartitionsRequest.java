package cl.duoc.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Nuevo numero total de particiones (solo se puede aumentar). */
public class PartitionsRequest {

    @NotNull(message = "El numero de particiones es obligatorio")
    @Min(value = 1, message = "Debe haber al menos 1 particion")
    @Max(value = 100, message = "Maximo 100 particiones")
    private Integer partitions;

    public Integer getPartitions() { return partitions; }
    public void setPartitions(Integer partitions) { this.partitions = partitions; }
}
