import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import CrudPage from '../views/CrudPage.vue'
import Fields from '../views/Fields.vue'
import Classifications from '../views/Classifications.vue'
import Rules from '../views/Rules.vue'
import MaskingPolicies from '../views/MaskingPolicies.vue'
import AccessRequests from '../views/AccessRequests.vue'
import AuditLogs from '../views/AuditLogs.vue'
import Users from '../views/Users.vue'
import AgentWorkbench from '../views/AgentWorkbench.vue'
import AgentFieldClassify from '../views/AgentFieldClassify.vue'
import AgentApprovalAdvice from '../views/AgentApprovalAdvice.vue'
import AgentRiskAlerts from '../views/AgentRiskAlerts.vue'
import AgentSecurityReport from '../views/AgentSecurityReport.vue'
import AgentChat from '../views/AgentChat.vue'

const routes = [
  { path: '/login', component: Login },
  { path: '/', component: Dashboard },
  { path: '/sources', component: CrudPage, props: { type: 'sources' } },
  { path: '/tables', component: CrudPage, props: { type: 'tables' } },
  { path: '/fields', component: Fields },
  { path: '/classifications', component: Classifications },
  { path: '/rules', component: Rules },
  { path: '/masking', component: MaskingPolicies },
  { path: '/requests', component: AccessRequests, props: { mode: 'user' } },
  { path: '/approvals', component: AccessRequests, props: { mode: 'approver' } },
  { path: '/audit', component: AuditLogs },
  { path: '/users', component: Users }
  ,{ path: '/agent', component: AgentWorkbench }
  ,{ path: '/agent/field-classify', component: AgentFieldClassify }
  ,{ path: '/agent/approval-advice', component: AgentApprovalAdvice }
  ,{ path: '/agent/risk-alerts', component: AgentRiskAlerts }
  ,{ path: '/agent/security-report', component: AgentSecurityReport }
  ,{ path: '/agent/chat', component: AgentChat }
]

const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach((to) => {
  if (to.path !== '/login' && !localStorage.getItem('token')) return '/login'
})
export default router
