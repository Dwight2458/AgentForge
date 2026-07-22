<script setup lang="ts">
defineProps<{ title: string }>()
</script>

<template>
  <div class="view-pad placeholder-view">
    <h1>{{ title }}</h1>
    <p>This module is scheduled after the executable golden path.</p>
    <RouterLink class="button primary" to="/projects">Return to Projects</RouterLink>
  </div>
</template>
