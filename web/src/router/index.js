import { createRouter, createWebHistory } from 'vue-router'
import ChatHome from '../views/ChatHome.vue'
import PptGenerator from '../views/PptGenerator.vue'
import ResearchView from '../views/ResearchView.vue'
import DocQAView from '../views/DocQAView.vue'

const routes = [
  { path: '/', name: 'chat', component: ChatHome },
  { path: '/ppt', name: 'ppt', component: PptGenerator },
  { path: '/research', name: 'research', component: ResearchView },
  { path: '/docqa', name: 'docqa', component: DocQAView },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
