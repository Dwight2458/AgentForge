export type Repository = {
  id: string
  fullName: string
  description: string
  branch: string
  source?: 'fixture' | 'github' | 'project'
  installationId?: number
  projectId?: string
}

export type AuthSession = {
  authenticated: boolean
  mode: 'development' | 'github'
  login: string | null
  name: string | null
  avatarUrl: string | null
  loginUrl: string | null
  installUrl: string | null
}

export type GithubInstallation = {
  id: number
  accountLogin: string
  accountAvatarUrl: string | null
  repositorySelection: string
  htmlUrl: string | null
}

export type GithubRepository = {
  id: number
  installationId: number
  fullName: string
  name: string
  description: string | null
  defaultBranch: string
  cloneUrl: string
  privateRepository: boolean
  htmlUrl: string
}

export type ApiProject = {
  id: string
  githubInstallationId: number
  repositoryFullName: string
  defaultBranch: string
  createdAt: string
}

export type ApiTask = {
  id: string
  projectId: string
  request: string
  status: string
  createdAt: string
}

export type RecentRun = {
  id: string
  task: string
  repository: string
  status: 'Succeeded' | 'Failed' | 'Running'
  started: string
  duration: string
}

export type GrillMessage = {
  id: number
  role: 'agent' | 'user'
  author: string
  time: string
  content: string
}

export type TimelineEvent = {
  id: number
  time: string
  agent: string
  summary: string
  detail?: string
  status: 'complete' | 'running' | 'pending'
}

export type SpecSection = {
  title: string
  items: string[]
}
