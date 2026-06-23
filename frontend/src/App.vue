<template>
  <router-view v-if="$route.path === '/login'" />
  <el-container v-else class="layout">
    <el-aside width="224px" class="aside">
      <div class="brand">数据分类分级保护系统</div>
      <el-menu router :default-active="$route.path" background-color="#0f172a" text-color="#cbd5e1" active-text-color="#fff">
        <el-menu-item index="/">首页看板</el-menu-item>
        <el-menu-item index="/sources">数据源管理</el-menu-item>
        <el-menu-item index="/tables">数据表管理</el-menu-item>
        <el-menu-item index="/fields">字段资产管理</el-menu-item>
        <el-menu-item index="/classifications">字段分类分级</el-menu-item>
        <el-menu-item index="/rules">分类规则管理</el-menu-item>
        <el-menu-item index="/masking">脱敏策略展示</el-menu-item>
        <el-menu-item index="/requests">数据访问申请</el-menu-item>
        <el-menu-item index="/approvals">审批管理</el-menu-item>
        <el-menu-item index="/audit">审计日志</el-menu-item>
        <el-menu-item index="/users">用户管理</el-menu-item>
        <el-menu-item index="/agent">智能体工作台</el-menu-item>
        <el-menu-item index="/agent/field-classify">字段智能分类</el-menu-item>
        <el-menu-item index="/agent/approval-advice">审批建议</el-menu-item>
        <el-menu-item index="/agent/risk-alerts">风险告警</el-menu-item>
        <el-menu-item index="/agent/security-report">安全报告</el-menu-item>
        <el-menu-item index="/agent/chat">Agent 问答</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>{{ user.real_name || user.username }}</span>
        <el-tag>{{ user.role_code }}</el-tag>
        <el-button text @click="logout">退出</el-button>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
  <FloatingAgent v-if="$route.path !== '/login'" />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from './api/http'
import FloatingAgent from './views/FloatingAgent.vue'

const router = useRouter()
const user = ref({})
onMounted(async () => { user.value = await api.get('/auth/me') })
function logout() {
  localStorage.clear()
  router.push('/login')
}
</script>
