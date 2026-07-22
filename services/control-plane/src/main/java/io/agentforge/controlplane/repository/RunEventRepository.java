package io.agentforge.controlplane.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.agentforge.controlplane.domain.RunEventEntity;

public interface RunEventRepository extends JpaRepository<RunEventEntity, UUID> {
    List<RunEventEntity> findByRunIdAndSequenceGreaterThanOrderBySequenceAsc(UUID runId, long sequence);
    Optional<RunEventEntity> findTopByRunIdOrderBySequenceDesc(UUID runId);
}

