package cl.duoc.pedidos360.kafkaadmin.dto;

/** Posicion de un grupo en una particion: offset confirmado, ultimo offset del topico y diferencia (lag). */
public record GroupPartitionLag(String topic, int partition, long currentOffset, long endOffset, long lag) {
}
