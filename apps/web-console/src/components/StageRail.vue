<script setup lang="ts">
import { Check } from '@lucide/vue'

const props = defineProps<{
  current: number
  cancelled?: boolean
}>()

const stages = ['Clarify', 'Plan', 'Run', 'Verify', 'Deliver']

function stageState(index: number) {
  const stage = index + 1
  if (props.cancelled && stage >= props.current) return 'cancelled'
  if (stage < props.current) return 'complete'
  if (stage === props.current) return 'active'
  return 'upcoming'
}
</script>

<template>
  <ol class="stage-rail" aria-label="Task progress">
    <li v-for="(stage, index) in stages" :key="stage" :class="stageState(index)">
      <span class="stage-node">
        <Check v-if="stageState(index) === 'complete'" :size="15" :stroke-width="2.5" />
        <template v-else>{{ index + 1 }}</template>
      </span>
      <span class="stage-copy">
        <strong>{{ stage }}</strong>
        <small>
          {{
            stageState(index) === 'complete'
              ? 'Complete'
              : stageState(index) === 'active'
                ? 'In progress'
                : stageState(index) === 'cancelled'
                  ? 'Cancelled'
                  : 'Upcoming'
          }}
        </small>
      </span>
      <span v-if="index < stages.length - 1" class="stage-line" />
    </li>
  </ol>
</template>
