import { createRouter, createWebHistory } from 'vue-router'

import GrillView from '@/views/GrillView.vue'
import PlaceholderView from '@/views/PlaceholderView.vue'
import ProjectsView from '@/views/ProjectsView.vue'
import RunView from '@/views/RunView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/projects' },
    { path: '/projects', component: ProjectsView },
    { path: '/tasks/:taskId/grill', component: GrillView },
    { path: '/runs/:runId', component: RunView },
    { path: '/skills', component: PlaceholderView, props: { title: 'Skills' } },
    { path: '/settings', component: PlaceholderView, props: { title: 'Settings' } },
    { path: '/:pathMatch(.*)*', redirect: '/projects' },
  ],
})
