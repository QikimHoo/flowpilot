import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import MainLayout from '../views/MainLayout.vue'
import PanoramaView from '../views/PanoramaView.vue'
import WarningCenterView from '../views/WarningCenterView.vue'
import ComplianceLedgerView from '../views/ComplianceLedgerView.vue'
import BaselineConfigView from '../views/BaselineConfigView.vue'
import ExportView from '../views/ExportView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: '', redirect: '/panorama' },
        { path: 'panorama', component: PanoramaView },
        { path: 'warnings', component: WarningCenterView },
        { path: 'compliance', component: ComplianceLedgerView },
        { path: 'baseline', component: BaselineConfigView },
        { path: 'export', component: ExportView }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const hasUser = !!localStorage.getItem('userId')
  if (to.path !== '/login' && !hasUser) {
    return '/login'
  }
  if (to.path === '/login' && hasUser) {
    return '/panorama'
  }
  return true
})

export default router
