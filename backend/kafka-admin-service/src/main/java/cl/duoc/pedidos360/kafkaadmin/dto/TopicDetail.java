package cl.duoc.pedidos360.kafkaadmin.dto;

import java.util.List;
import java.util.Map;

public record TopicDetail(String name, int replicationFactor, List<PartitionInfo> partitions, Map<String, String> configs) {
}
