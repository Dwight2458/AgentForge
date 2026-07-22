package io.agentforge.controlplane.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class ProjectEntity {

    @Id
    private UUID id;

    @Column(name = "github_installation_id", nullable = false)
    private long githubInstallationId;

    @Column(name = "repository_full_name", nullable = false, unique = true)
    private String repositoryFullName;

    @Column(name = "clone_url", nullable = false)
    private String cloneUrl;

    @Column(name = "default_branch", nullable = false)
    private String defaultBranch;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProjectEntity() {}

    public ProjectEntity(long githubInstallationId, String repositoryFullName, String cloneUrl, String defaultBranch) {
        this.id = UUID.randomUUID();
        this.githubInstallationId = githubInstallationId;
        this.repositoryFullName = repositoryFullName;
        this.cloneUrl = cloneUrl;
        this.defaultBranch = defaultBranch;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public long getGithubInstallationId() { return githubInstallationId; }
    public String getRepositoryFullName() { return repositoryFullName; }
    public String getCloneUrl() { return cloneUrl; }
    public String getDefaultBranch() { return defaultBranch; }
    public Instant getCreatedAt() { return createdAt; }
}

