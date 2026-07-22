package io.agentforge.controlplane.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import io.agentforge.controlplane.domain.OutboxEventEntity;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findByPublishedAtIsNullOrderByOccurredAtAsc(Pageable pageable);
}

