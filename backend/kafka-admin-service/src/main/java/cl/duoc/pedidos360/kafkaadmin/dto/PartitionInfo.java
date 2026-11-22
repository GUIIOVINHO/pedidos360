package cl.duoc.pedidos360.kafkaadmin.dto;

import java.util.List;

public record PartitionInfo(int partition, Integer leader, List<Integer> replicas, List<Integer> isr) {
}
