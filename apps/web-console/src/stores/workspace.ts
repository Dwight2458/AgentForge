import { defineStore } from 'pinia'

import { api, ApiError } from '@/api/client'
import { initialMessages, repositories as fixtureRepositories } from '@/data/fixtures'
import type { AuthSession, GrillMessage, Repository } from '@/types'

export const useWorkspaceStore = defineStore('workspace', {
  state: () => ({
    selectedRepositoryId: 'mall-service',
    repositories: fixtureRepositories.map((repository) => ({ ...repository, source: 'fixture' })) as Repository[],
    session: null as AuthSession | null,
    connectionStatus: 'idle' as 'idle' | 'loading' | 'ready' | 'error',
    connectionError: null as string | null,
    taskRequest:
      'Add payment-status filtering to the order list. Update the Spring Boot API, Vue page, and automated tests.',
    messages: initialMessages.map((message) => ({ ...message })) as GrillMessage[],
    runCancelled: false,
    savedAt: null as string | null,
  }),
  actions: {
    async loadConnections() {
      if (this.connectionStatus === 'loading' || this.connectionStatus === 'ready') return
      this.connectionStatus = 'loading'
      this.connectionError = null
      try {
        this.session = await api.session()
        if (this.session.authenticated) {
          const installations = await api.installations()
          const repositories = (
            await Promise.all(installations.map((installation) => api.repositories(installation.id)))
          ).flat()
          this.repositories = repositories.map((repository) => ({
            id: `github-${repository.id}`,
            fullName: repository.fullName,
            description: repository.description ?? 'GitHub repository',
            branch: repository.defaultBranch,
            source: 'github',
            installationId: repository.installationId,
          }))
        } else {
          const projects = await api.projects()
          if (projects.length > 0) {
            this.repositories = projects.map((project) => ({
              id: `project-${project.id}`,
              fullName: project.repositoryFullName,
              description: 'Imported AgentForge project',
              branch: project.defaultBranch,
              source: 'project',
              installationId: project.githubInstallationId,
              projectId: project.id,
            }))
          }
        }
        if (!this.repositories.some((repository) => repository.id === this.selectedRepositoryId)) {
          this.selectedRepositoryId = this.repositories[0]?.id ?? ''
        }
        this.connectionStatus = 'ready'
      } catch (error) {
        this.connectionStatus = 'error'
        this.connectionError = error instanceof Error ? error.message : 'Unable to load GitHub repositories'
      }
    },
    async createTask(): Promise<string> {
      const selected = this.repositories.find((repository) => repository.id === this.selectedRepositoryId)
      if (!selected) throw new Error('Select a repository before creating a task')
      if (selected.source === 'fixture') return 'task-1042'

      let projectId = selected.projectId
      if (!projectId) {
        if (!selected.installationId) throw new Error('The GitHub installation is unavailable')
        try {
          const project = await api.importProject(selected.installationId, selected.fullName)
          projectId = project.id
        } catch (error) {
          if (!(error instanceof ApiError) || error.status !== 409) throw error
          const project = (await api.projects()).find(
            (candidate) => candidate.repositoryFullName.toLowerCase() === selected.fullName.toLowerCase(),
          )
          if (!project) throw error
          projectId = project.id
        }
        selected.projectId = projectId
        selected.source = 'project'
      }
      const task = await api.createTask(projectId, this.taskRequest)
      return task.id
    },
    addMessage(content: string) {
      const now = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      this.messages.push({
        id: Date.now(),
        role: 'user',
        author: 'You',
        time: now,
        content,
      })
    },
    saveDraft() {
      this.savedAt = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    },
    cancelRun() {
      this.runCancelled = true
    },
  },
})
