package cl.duoc.pedidos360.kafkaadmin.dto;

import java.util.List;

public record ConsumerGroupDetail(String groupId, String state, String partitionAssignor,
                                  List<GroupMemberInfo> members, List<GroupPartitionLag> partitions, long totalLag) {
}
