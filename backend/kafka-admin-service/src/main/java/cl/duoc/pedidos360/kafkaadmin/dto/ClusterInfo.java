package cl.duoc.pedidos360.kafkaadmin.dto;

import java.util.List;

public record ClusterInfo(String clusterId, Integer controllerId, List<BrokerInfo> brokers) {
}
