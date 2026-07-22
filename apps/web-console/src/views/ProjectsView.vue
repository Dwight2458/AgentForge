<script setup lang="ts">
import { CheckCircle2, ChevronRight, Plus, Search, SlidersHorizontal } from '@lucide/vue'
import { computed, ref } from 'vue'
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

import { recentRuns } from '@/data/fixtures'
import GithubMark from '@/components/GithubMark.vue'
import { useWorkspaceStore } from '@/stores/workspace'

const router = useRouter()
const workspace = useWorkspaceStore()
const query = ref('')
const creatingTask = ref(false)
const taskError = ref<string | null>(null)

onMounted(() => void workspace.loadConnections())

const filteredRepositories = computed(() => {
  const search = query.value.trim().toLowerCase()
  if (!search) return workspace.repositories
  return workspace.repositories.filter((repository) =>
    `${repository.fullName} ${repository.description}`.toLowerCase().includes(search),
  )
})

async function startClarification() {
  if (!workspace.taskRequest.trim()) return
  creatingTask.value = true
  taskError.value = null
  try {
    const taskId = await workspace.createTask()
    await router.push(`/tasks/${taskId}/grill`)
  } catch (error) {
    taskError.value = error instanceof Error ? error.message : 'Unable to create the task'
  } finally {
    creatingTask.value = false
  }
}
</script>

<template>
  <div class="projects-view view-pad">
    <header class="page-header">
      <h1>Projects</h1>
      <div class="github-actions">
        <span v-if="workspace.session?.authenticated" class="connection-state connected">
          <CheckCircle2 :size="15" /> GitHub connected
        </span>
        <a
          v-else-if="workspace.session?.loginUrl"
          class="button secondary"
          :href="workspace.session.loginUrl"
        >
          Connect GitHub
        </a>
        <a
          v-if="workspace.session?.installUrl"
          class="button secondary"
          :href="workspace.session.installUrl"
          target="_blank"
          rel="noreferrer"
        >
          Install app
        </a>
      </div>
    </header>

    <p v-if="workspace.connectionError" class="inline-error" role="alert">
      {{ workspace.connectionError }} — showing the local development catalog.
    </p>

    <section class="projects-workspace">
      <div class="surface repository-panel">
        <div class="surface-heading">
          <h2>Repositories</h2>
          <a
            class="icon-button"
            :href="workspace.session?.installUrl ?? '#connect-github'"
            aria-label="Install GitHub App or import repository"
          >
            <Plus :size="18" />
          </a>
        </div>
        <label class="search-field">
          <Search :size="17" />
          <input v-model="query" type="search" placeholder="Filter repositories…" />
          <SlidersHorizontal :size="16" />
        </label>
        <div class="repository-list">
          <button
            v-for="repository in filteredRepositories"
            :key="repository.id"
            class="repository-row"
            :class="{ selected: workspace.selectedRepositoryId === repository.id }"
            type="button"
            @click="workspace.selectedRepositoryId = repository.id"
          >
            <GithubMark :size="21" />
            <span class="repository-copy">
              <strong>{{ repository.fullName }}</strong>
              <small>{{ repository.description }}</small>
            </span>
            <span class="branch">{{ repository.branch }}</span>
            <ChevronRight :size="17" />
          </button>
        </div>
        <div class="surface-footer">{{ filteredRepositories.length }} repositories</div>
      </div>

      <form class="surface task-composer" @submit.prevent="startClarification">
        <div class="surface-heading">
          <h2>New task</h2>
        </div>
        <label class="field-label" for="repository">Repository</label>
        <select id="repository" v-model="workspace.selectedRepositoryId" class="control">
          <option v-for="repository in workspace.repositories" :key="repository.id" :value="repository.id">
            {{ repository.fullName }} · {{ repository.branch }}
          </option>
        </select>

        <label class="field-label" for="request">Development request</label>
        <div class="textarea-shell">
          <textarea
            id="request"
            v-model="workspace.taskRequest"
            maxlength="4000"
            placeholder="Describe the feature, bug fix, or improvement you want to build…"
          />
          <span>{{ workspace.taskRequest.length }} / 4000</span>
        </div>

        <label class="field-label" for="profile">Resource profile</label>
        <select id="profile" class="control">
          <option>Standard · 4 CPU / 6 GiB RAM</option>
        </select>

        <div class="composer-actions">
          <span class="estimate"><CheckCircle2 :size="16" /> Estimated runtime: 10–25 min</span>
          <button
            class="button primary"
            type="submit"
            :disabled="!workspace.taskRequest.trim() || creatingTask"
          >
            {{ creatingTask ? 'Creating…' : 'Start clarification' }}
          </button>
        </div>
        <p v-if="taskError" class="form-error" role="alert">{{ taskError }}</p>
      </form>
    </section>

    <section class="surface recent-runs">
      <div class="surface-heading">
        <h2>Recent runs</h2>
      </div>
      <div class="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Run ID</th>
              <th>Task</th>
              <th>Repository</th>
              <th>Status</th>
              <th>Started</th>
              <th>Duration</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="run in recentRuns" :key="run.id">
              <td class="mono">{{ run.id }}</td>
              <td>{{ run.task }}</td>
              <td>{{ run.repository }}</td>
              <td><span class="status-text" :class="run.status.toLowerCase()">{{ run.status }}</span></td>
              <td>{{ run.started }}</td>
              <td>{{ run.duration }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <button class="text-button" type="button">View all runs <ChevronRight :size="15" /></button>
    </section>
  </div>
</template>
