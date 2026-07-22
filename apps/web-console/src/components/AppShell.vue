<script setup lang="ts">
import { CircleHelp, Folder, ListTodo, PlayCircle, Settings, Sparkles } from '@lucide/vue'
import { computed, onMounted } from 'vue'

import BrandMark from './BrandMark.vue'
import { useWorkspaceStore } from '@/stores/workspace'

const workspace = useWorkspaceStore()
const profileName = computed(() => workspace.session?.name ?? workspace.session?.login ?? 'Local developer')
const profileSubtitle = computed(() =>
  workspace.session?.authenticated ? 'GitHub connected' : 'Local workspace',
)
const initials = computed(() =>
  profileName.value
    .split(/\s+/)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase(),
)

onMounted(() => void workspace.loadConnections())

const navigation = [
  { label: 'Projects', path: '/projects', icon: Folder },
  { label: 'Tasks', path: '/tasks/task-1042/grill', icon: ListTodo },
  { label: 'Runs', path: '/runs/AF-1042', icon: PlayCircle },
  { label: 'Skills', path: '/skills', icon: Sparkles },
  { label: 'Settings', path: '/settings', icon: Settings },
]
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <RouterLink class="brand" to="/projects" aria-label="AgentForge Projects">
        <BrandMark />
        <span>AgentForge</span>
      </RouterLink>

      <nav class="primary-nav" aria-label="Primary navigation">
        <RouterLink v-for="item in navigation" :key="item.label" :to="item.path" class="nav-item">
          <component :is="item.icon" :size="18" :stroke-width="1.75" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <a class="nav-item" href="/docs" @click.prevent>
          <CircleHelp :size="18" :stroke-width="1.75" />
          <span>Help</span>
        </a>
        <div class="profile">
          <img
            v-if="workspace.session?.avatarUrl"
            class="avatar avatar-image"
            :src="workspace.session.avatarUrl"
            alt=""
          />
          <div v-else class="avatar">{{ initials }}</div>
          <div>
            <strong>{{ profileName }}</strong>
            <span>{{ profileSubtitle }}</span>
          </div>
        </div>
      </div>
    </aside>
    <main class="app-main">
      <slot />
    </main>
  </div>
</template>
