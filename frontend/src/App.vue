<template>
  <router-view v-if="$route.path === '/login'" />
  <el-container v-else class="layout">
    <el-aside width="224px" class="aside">
      <div class="brand">数据分类分级保护系统</div>
      <el-menu router :default-active="$route.path" background-color="#0f172a" text-color="#cbd5e1" active-text-color="#fff">
        <div class="menu-section">治理总览</div>
        <el-menu-item index="/"><span class="nav-mark">览</span><span>首页看板</span></el-menu-item>
        <div class="menu-section">数据资产</div>
        <el-menu-item index="/sources"><span class="nav-mark">源</span><span>数据源管理</span></el-menu-item>
        <el-menu-item index="/tables"><span class="nav-mark">表</span><span>数据表管理</span></el-menu-item>
        <el-menu-item index="/fields"><span class="nav-mark">字</span><span>字段资产管理</span></el-menu-item>
        <div class="menu-section">分类与访问</div>
        <el-menu-item index="/classifications"><span class="nav-mark">级</span><span>字段分类分级</span></el-menu-item>
        <el-menu-item index="/rules"><span class="nav-mark">规</span><span>分类规则管理</span></el-menu-item>
        <el-menu-item index="/masking"><span class="nav-mark">敏</span><span>脱敏策略展示</span></el-menu-item>
        <el-menu-item index="/requests"><span class="nav-mark">申</span><span>数据访问申请</span></el-menu-item>
        <el-menu-item index="/approvals"><span class="nav-mark">批</span><span>审批管理</span></el-menu-item>
        <el-menu-item index="/audit"><span class="nav-mark">审</span><span>审计日志</span></el-menu-item>
        <el-menu-item index="/users"><span class="nav-mark">人</span><span>用户管理</span></el-menu-item>
        <div class="menu-section">安全 Agent</div>
        <el-menu-item index="/agent"><span class="nav-mark">AI</span><span>智能体工作台</span></el-menu-item>
        <el-menu-item index="/agent/field-classify"><span class="nav-mark">分</span><span>字段智能分类</span></el-menu-item>
        <el-menu-item index="/agent/approval-advice"><span class="nav-mark">议</span><span>审批建议</span></el-menu-item>
        <el-menu-item index="/agent/risk-alerts"><span class="nav-mark">险</span><span>风险告警</span></el-menu-item>
        <el-menu-item index="/agent/security-report"><span class="nav-mark">报</span><span>安全报告</span></el-menu-item>
        <el-menu-item index="/agent/chat"><span class="nav-mark">问</span><span>Agent 问答</span></el-menu-item>
      </el-menu>
      <div class="aside-status">
        <span></span>
        <div>
          <strong>演示环境在线</strong>
          <small>H2 + Local Agent</small>
        </div>
      </div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-context">
          <strong>{{ routeTitle }}</strong>
          <span>{{ routeHint }}</span>
        </div>
        <el-select
          v-model="quickPath"
          class="quick-jump"
          filterable
          clearable
          placeholder="快速跳转"
          @change="goQuick"
        >
          <el-option v-for="item in quickRoutes" :key="item.path" :label="item.title" :value="item.path">
            <div class="quick-option">
              <strong>{{ item.title }}</strong>
              <span>{{ item.hint }}</span>
            </div>
          </el-option>
        </el-select>
        <span class="header-user">{{ user.real_name || user.username }}</span>
        <el-tag>{{ user.role_code }}</el-tag>
        <el-button text @click="logout">退出</el-button>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
  <FloatingAgent v-if="$route.path !== '/login'" />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from './api/http'
import FloatingAgent from './views/FloatingAgent.vue'

const router = useRouter()
const route = useRoute()
const user = ref({})
const quickPath = ref('')
const routeMeta = {
  '/': ['首页看板', '治理态势与快捷操作'],
  '/sources': ['数据源管理', '维护企业数据入口'],
  '/tables': ['数据表管理', '承载业务表资产'],
  '/fields': ['字段资产管理', '字段级敏感识别与访问申请'],
  '/classifications': ['字段分类分级', '人工、规则和 Agent 分类'],
  '/rules': ['分类规则管理', '关键词和正则规则'],
  '/masking': ['脱敏策略展示', '敏感样例值保护'],
  '/requests': ['数据访问申请', '普通用户高敏访问流程'],
  '/approvals': ['审批管理', '审批人与 Agent 建议协同'],
  '/audit': ['审计日志', '关键操作全链路留痕'],
  '/users': ['用户管理', '演示角色与账号维护'],
  '/agent': ['智能体工作台', '建议、告警和任务汇总'],
  '/agent/field-classify': ['字段智能分类', 'Agent 分类建议与应用'],
  '/agent/approval-advice': ['审批建议', '高敏访问风险评估'],
  '/agent/risk-alerts': ['风险告警', '审计日志风险分析'],
  '/agent/security-report': ['安全报告', 'Markdown 治理报告生成'],
  '/agent/chat': ['Agent 问答', '自然语言治理助手']
}
const quickRoutes = computed(() => Object.entries(routeMeta).map(([path, meta]) => ({ path, title: meta[0], hint: meta[1] })))
const routeTitle = computed(() => routeMeta[route.path]?.[0] || '数据分类分级保护系统')
const routeHint = computed(() => routeMeta[route.path]?.[1] || '企业数据安全治理平台')
onMounted(async () => { user.value = await api.get('/auth/me') })
function goQuick(path) {
  if (!path) return
  router.push(path)
  quickPath.value = ''
}
function logout() {
  localStorage.clear()
  router.push('/login')
}
</script>
