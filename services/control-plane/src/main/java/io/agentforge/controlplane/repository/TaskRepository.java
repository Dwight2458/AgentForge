package io.agentforge.controlplane.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.agentforge.controlplane.domain.TaskEntity;

public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {
    List<TaskEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}

