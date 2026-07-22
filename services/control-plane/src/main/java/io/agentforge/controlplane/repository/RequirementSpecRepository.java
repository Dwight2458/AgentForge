package io.agentforge.controlplane.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.agentforge.controlplane.domain.RequirementSpecEntity;

public interface RequirementSpecRepository extends JpaRepository<RequirementSpecEntity, UUID> {}

