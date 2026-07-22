package io.agentforge.controlplane.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.agentforge.controlplane.domain.ProjectEntity;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {
    boolean existsByRepositoryFullName(String repositoryFullName);
}

