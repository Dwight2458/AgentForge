<script setup lang="ts">
import {
  Check,
  Circle,
  Code2,
  ExternalLink,
  FileCode2,
  Filter,
  GitBranch,
  PackageOpen,
  TestTube2,
  X,
} from '@lucide/vue'
import { computed, ref } from 'vue'

import StageRail from '@/components/StageRail.vue'
import { timelineEvents } from '@/data/fixtures'
import { useWorkspaceStore } from '@/stores/workspace'

type Tab = 'Diff' | 'Tests' | 'Artifacts'

const workspace = useWorkspaceStore()
const activeTab = ref<Tab>('Diff')
const hideCompleted = ref(false)
const visibleEvents = computed(() =>
  hideCompleted.value ? timelineEvents.filter((event) => event.status !== 'complete') : timelineEvents,
)

const changedFiles = [
  ['src/main/java/com/mall/order/OrderQueryService.java', '+24', '−6'],
  ['src/main/java/com/mall/order/OrderRepository.java', '+18', '−2'],
  ['frontend/src/views/orders/OrderListView.vue', '+31', '−4'],
  ['frontend/src/api/orders.ts', '+8', '−1'],
]

const tests = [
  ['OrderQueryServiceTest', '12 passed', '2.1s'],
  ['OrderRepositoryTest', '8 passed', '1.4s'],
  ['OrderListFilter.spec.ts', 'Running', '—'],
  ['OrdersApi.spec.ts', 'Queued', '—'],
]
</script>

<template>
  <div class="workflow-view run-view">
    <header class="run-header">
      <div class="run-title-row">
        <div>
          <div class="breadcrumb"><strong>mall-service</strong><span>/</span>Run AF-1042</div>
          <div class="run-title">
            <h1>Payment status filter</h1>
            <span class="run-status" :class="{ cancelled: workspace.runCancelled }">
              {{ workspace.runCancelled ? 'Cancelled' : 'Running' }}
            </span>
          </div>
        </div>
        <div class="run-actions">
          <button
            class="button danger"
            type="button"
            :disabled="workspace.runCancelled"
            @click="workspace.cancelRun"
          >
            <X :size="16" /> {{ workspace.runCancelled ? 'Run cancelled' : 'Cancel run' }}
          </button>
          <button class="button secondary" type="button" disabled>
            <ExternalLink :size="16" /> Open preview
          </button>
        </div>
      </div>
      <StageRail :current="3" :cancelled="workspace.runCancelled" />
    </header>

    <div class="run-workspace">
      <section class="timeline-pane">
        <header class="panel-toolbar">
          <h2>Execution timeline</h2>
          <div>
            <span class="live-state"><i /> {{ workspace.runCancelled ? 'Stopped' : 'Live' }}</span>
            <button class="button compact secondary" type="button" @click="hideCompleted = !hideCompleted">
              <Filter :size="14" /> {{ hideCompleted ? 'Show all' : 'Filter' }}
            </button>
          </div>
        </header>

        <div class="timeline-events">
          <article v-for="event in visibleEvents" :key="event.id" class="timeline-event" :class="event.status">
            <div class="event-marker">
              <Check v-if="event.status === 'complete'" :size="12" />
              <Circle v-else :size="11" fill="currentColor" />
            </div>
            <time>{{ event.time }}</time>
            <div class="event-icon">
              <GitBranch v-if="event.agent.includes('Repository') || event.agent === 'Supervisor'" :size="17" />
              <Code2 v-else-if="event.agent.includes('Developer')" :size="17" />
              <TestTube2 v-else :size="17" />
            </div>
            <div class="event-copy">
              <strong>{{ event.agent }}</strong>
              <p>{{ event.summary }}</p>
              <small v-if="event.detail">{{ event.detail }}</small>
            </div>
          </article>
        </div>

        <section class="terminal-panel">
          <header><span>Live terminal <i /></span><button type="button">Clear</button></header>
          <pre><span class="terminal-time">12:22:35</span> [frontend-developer] npm run type-check
  &gt; mall-frontend@0.1.0 type-check
  &gt; vue-tsc --noEmit
<span class="terminal-success">✓ No type errors found</span>
<span class="terminal-time">12:22:36</span> [frontend-developer] npm run test:unit
  &gt; vitest run
<span class="terminal-success">✓ 18 tests passed</span>
<span class="terminal-time">12:22:37</span> [frontend-developer] <span class="cursor">▋</span></pre>
        </section>
      </section>

      <aside class="run-inspector">
        <section class="topology-panel">
          <header class="panel-toolbar"><h2>Subagent topology</h2></header>
          <div class="topology" aria-label="Subagent topology">
            <div class="agent-node orchestrator"><GitBranch :size="16" /> Supervisor <i /></div>
            <div class="topology-lines" />
            <div class="agent-row">
              <div class="agent-node"><GitBranch :size="15" /> Repo Analyst <i class="done" /></div>
              <div class="agent-node"><Code2 :size="15" /> Backend Dev <i class="done" /></div>
              <div class="agent-node"><Code2 :size="15" /> Frontend Dev <i /></div>
              <div class="agent-node muted"><TestTube2 :size="15" /> Test Engineer <i /></div>
            </div>
          </div>
        </section>

        <section class="evidence-panel">
          <div class="tabs" role="tablist">
            <button
              v-for="tab in (['Diff', 'Tests', 'Artifacts'] as Tab[])"
              :key="tab"
              type="button"
              role="tab"
              :aria-selected="activeTab === tab"
              :class="{ active: activeTab === tab }"
              @click="activeTab = tab"
            >
              {{ tab }}
            </button>
          </div>

          <div v-if="activeTab === 'Diff'" class="tab-content">
            <div class="section-summary"><strong>Changed files</strong><span class="additions">+81</span><span class="deletions">−13</span></div>
            <div v-for="file in changedFiles" :key="file[0]" class="file-row">
              <FileCode2 :size="15" />
              <span>{{ file[0] }}</span>
              <b class="additions">{{ file[1] }}</b>
              <b class="deletions">{{ file[2] }}</b>
            </div>
            <div class="embedded-tests">
              <div class="section-summary"><strong>Tests</strong><span>20 passed · 2 running</span></div>
              <div class="test-progress"><span style="width: 82%" /></div>
              <div v-for="test in tests" :key="test[0]" class="test-row">
                <Check v-if="test[1].includes('passed')" :size="14" />
                <Circle v-else :size="11" />
                <span>{{ test[0] }}</span><b>{{ test[1] }}</b><small>{{ test[2] }}</small>
              </div>
            </div>
          </div>

          <div v-else-if="activeTab === 'Tests'" class="tab-content">
            <div class="test-progress"><span style="width: 82%" /></div>
            <div v-for="test in tests" :key="test[0]" class="test-row">
              <Check v-if="test[1].includes('passed')" :size="14" />
              <Circle v-else :size="11" />
              <span>{{ test[0] }}</span><b>{{ test[1] }}</b><small>{{ test[2] }}</small>
            </div>
          </div>

          <div v-else class="tab-content artifact-empty">
            <PackageOpen :size="28" />
            <strong>Artifacts arrive after verification</strong>
            <p>Screenshots, test reports, and the delivery patch will appear here.</p>
          </div>
        </section>
      </aside>
    </div>

    <footer class="budget-footer">
      <div><strong>Elapsed</strong><span>00:01:34</span></div>
      <div><strong>Tool calls</strong><span>24 / 150</span></div>
      <div><strong>Tokens</strong><span>32,841 / 200k</span></div>
      <div class="budget-meter"><strong>Budget</strong><span>$0.18 / $5.00</span><i><b /></i><small>4%</small></div>
    </footer>
  </div>
</template>
