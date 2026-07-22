import type {
  ApiProject,
  ApiTask,
  AuthSession,
  GithubInstallation,
  GithubRepository,
} from '@/types'

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message)
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    credentials: 'include',
    headers: { Accept: 'application/json', ...init?.headers },
    ...init,
  })
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as { message?: string } | null
    throw new ApiError(response.status, body?.message ?? `Request failed with HTTP ${response.status}`)
  }
  return (await response.json()) as T
}

async function csrfHeaders(): Promise<Record<string, string>> {
  const csrf = await request<{ enabled: boolean; headerName: string | null; token: string | null }>(
    '/api/v1/auth/csrf',
  )
  return csrf.enabled && csrf.headerName && csrf.token ? { [csrf.headerName]: csrf.token } : {}
}

async function post<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(await csrfHeaders()) },
    body: JSON.stringify(body),
  })
}

export const api = {
  session: () => request<AuthSession>('/api/v1/auth/session'),
  installations: () => request<GithubInstallation[]>('/api/v1/github/installations'),
  repositories: (installationId: number) =>
    request<GithubRepository[]>(`/api/v1/github/installations/${installationId}/repositories`),
  projects: () => request<ApiProject[]>('/api/v1/projects'),
  importProject: (githubInstallationId: number, repositoryFullName: string) =>
    post<ApiProject>('/api/v1/projects/import', { githubInstallationId, repositoryFullName }),
  createTask: (projectId: string, developmentRequest: string) =>
    post<ApiTask>(`/api/v1/projects/${projectId}/tasks`, { request: developmentRequest }),
}

