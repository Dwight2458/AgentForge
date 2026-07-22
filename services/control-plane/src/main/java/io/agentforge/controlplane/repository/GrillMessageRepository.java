package io.agentforge.controlplane.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.agentforge.controlplane.domain.GrillMessageEntity;

public interface GrillMessageRepository extends JpaRepository<GrillMessageEntity, UUID> {
    List<GrillMessageEntity> findByTaskIdOrderByCreatedAtAsc(UUID taskId);
    long countByTaskIdAndRole(UUID taskId, String role);
}

