<script setup lang="ts">
import { CheckCircle2, Pencil, Plus, Save, Send, Sparkles, UserRound } from '@lucide/vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import StageRail from '@/components/StageRail.vue'
import { specSections } from '@/data/fixtures'
import { useWorkspaceStore } from '@/stores/workspace'

const workspace = useWorkspaceStore()
const router = useRouter()
const answer = ref('')

function submitAnswer() {
  const content = answer.value.trim()
  if (!content) return
  workspace.addMessage(content)
  answer.value = ''
}

function finalize() {
  submitAnswer()
  void router.push('/runs/AF-1042')
}
</script>

<template>
  <div class="workflow-view grill-view">
    <header class="workflow-header">
      <div class="breadcrumb"><strong>mall-service</strong><span>/</span>Add payment status filter</div>
      <StageRail :current="1" />
    </header>

    <div class="grill-layout">
      <section class="conversation-pane">
        <div class="pane-title">
          <h1>Clarify the task</h1>
          <p>Requirement Agent is asking questions to fully understand the request.</p>
        </div>

        <div class="conversation" aria-live="polite">
          <article v-for="message in workspace.messages" :key="message.id" class="message">
            <div class="message-avatar" :class="message.role">
              <Sparkles v-if="message.role === 'agent'" :size="17" />
              <UserRound v-else :size="17" />
            </div>
            <div class="message-body">
              <div class="message-meta"><strong>{{ message.author }}</strong><span>{{ message.time }}</span></div>
              <p>{{ message.content }}</p>
            </div>
          </article>
        </div>

        <form class="answer-composer" @submit.prevent="submitAnswer">
          <textarea v-model="answer" placeholder="Answer the question or add more details…" />
          <div class="answer-actions">
            <span>Be specific about edge cases and verification.</span>
            <button class="send-button" type="submit" :disabled="!answer.trim()" aria-label="Send answer">
              <Send :size="17" />
            </button>
          </div>
        </form>
      </section>

      <aside class="spec-pane">
        <div class="spec-heading">
          <div><h2>Requirement Spec</h2><span class="draft-state">Draft</span></div>
          <small>{{ workspace.savedAt ? `Saved at ${workspace.savedAt}` : 'Updated just now' }}</small>
        </div>

        <section v-for="section in specSections" :key="section.title" class="spec-section">
          <header>
            <CheckCircle2 :size="18" />
            <h3>{{ section.title }}</h3>
          </header>
          <ol>
            <li v-for="item in section.items" :key="item">
              <span>{{ item }}</span>
              <button type="button" aria-label="Edit item"><Pencil :size="14" /></button>
            </li>
          </ol>
          <button class="add-row" type="button"><Plus :size="15" /> Add {{ section.title.toLowerCase() }} item</button>
        </section>

        <footer class="spec-actions">
          <button class="button secondary" type="button" @click="workspace.saveDraft">
            <Save :size="16" /> Save draft
          </button>
          <button class="button primary finalize-button" type="button" @click="finalize">
            <Send :size="16" /> Finalize and run automatically
          </button>
        </footer>
      </aside>
    </div>
  </div>
</template>
