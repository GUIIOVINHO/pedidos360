package cl.duoc.pedidos360.kafkaadmin.dto;

import java.util.List;

public record GroupMemberInfo(String memberId, String clientId, String host, List<String> partitions) {
}
