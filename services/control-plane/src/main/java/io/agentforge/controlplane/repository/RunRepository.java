package io.agentforge.controlplane.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.agentforge.controlplane.domain.RunEntity;

public interface RunRepository extends JpaRepository<RunEntity, UUID> {
    List<RunEntity> findTop20ByOrderByCreatedAtDesc();
}

