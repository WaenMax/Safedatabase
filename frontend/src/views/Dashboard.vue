<template>
  <div class="page-head">
    <div>
      <h1 class="page-title">首页统计看板</h1>
      <p>数据安全治理闭环运行态势</p>
    </div>
    <el-tag :type="riskTone.type" size="large">{{ riskTone.text }}</el-tag>
  </div>

  <div class="governance-flow">
    <div v-for="(step, index) in flowSteps" :key="step.title" class="flow-step clickable" @click="$router.push(step.path)">
      <span>{{ index + 1 }}</span>
      <strong>{{ step.title }}</strong>
    </div>
  </div>

  <div class="stat-grid cockpit-grid">
    <div v-for="card in statCards" :key="card.label" class="stat clickable" :class="card.accent" @click="$router.push(card.path)">
      {{ card.label }}<strong>{{ card.value }}</strong><span>{{ card.desc }}</span><em>进入</em>
    </div>
  </div>

  <div class="toolbar">
    <el-button type="primary" @click="$router.push('/fields')">新增字段资产</el-button>
    <el-button type="success" @click="autoClassify">执行自动分类</el-button>
    <el-button type="warning" @click="$router.push('/approvals')">查看待审批申请</el-button>
    <el-button @click="$router.push('/agent')">打开 Agent 工作台</el-button>
  </div>
  <el-row :gutter="16">
    <el-col :span="12">
      <el-card>
        <template #header>分类分级态势</template>
        <div class="level-bars">
          <div v-for="row in normalizedLevelStats" :key="row.name" class="level-row">
            <div class="level-label"><strong>{{ row.name }}</strong><span>{{ levelText(row.name) }}</span></div>
            <div class="level-track"><i :class="`level-${row.name}`" :style="{ width: `${row.percent}%` }"></i></div>
            <b>{{ row.cnt }}</b>
          </div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card>
        <template #header>数据分类构成</template>
        <div class="category-list">
          <div v-for="row in normalizedCategoryStats" :key="row.name" class="category-item">
            <span>{{ row.name }}</span>
            <strong>{{ row.cnt }}</strong>
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
  <el-card style="margin-top:16px">
    <template #header>最近审计日志</template>
    <el-timeline>
      <el-timeline-item v-for="log in data.recentAuditLogs" :key="log.ID" :type="log.RESULT === 'SUCCESS' ? 'success' : 'danger'" :timestamp="log.OPERATION_TIME">
        <strong>{{ log.OPERATION_TYPE }}</strong>
        <span class="audit-user">{{ log.USERNAME || 'system' }}</span>
        <p>{{ log.DETAIL }}</p>
      </el-timeline-item>
    </el-timeline>
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/http'
const data = ref({})
const flowSteps = [
  { title: '资产登记', path: '/fields' },
  { title: '分类分级', path: '/classifications' },
  { title: '脱敏访问', path: '/masking' },
  { title: '申请审批', path: '/approvals' },
  { title: '审计分析', path: '/audit' },
  { title: 'Agent 整改', path: '/agent' }
]
const statCards = computed(() => [
  { label: '数据源数量', value: data.value.sourceCount, desc: '已纳入台账', accent: 'accent-blue', path: '/sources' },
  { label: '数据表数量', value: data.value.tableCount, desc: '业务表资产', accent: 'accent-green', path: '/tables' },
  { label: '字段资产数量', value: data.value.fieldCount, desc: '字段级治理对象', accent: 'accent-indigo', path: '/fields' },
  { label: '敏感字段数量', value: data.value.sensitiveFieldCount, desc: '默认脱敏展示', accent: 'accent-cyan', path: '/fields' },
  { label: 'L3/L4/L5 高敏字段', value: data.value.highSensitiveFieldCount, desc: '审批和审计重点', accent: 'accent-orange', path: '/classifications' },
  { label: '待审批申请', value: data.value.pendingRequestCount, desc: '需要负责人处理', accent: 'accent-amber', path: '/approvals' },
  { label: '未处理风险告警', value: data.value.openRiskAlertCount, desc: '建议优先核查', accent: 'accent-red', path: '/agent/risk-alerts' },
  { label: '分类覆盖率', value: `${classifyCoverage.value}%`, desc: '已分类字段占比', accent: 'accent-slate', path: '/classifications' }
])
const normalizedLevelStats = computed(() => normalizeStats(data.value.levelStats))
const normalizedCategoryStats = computed(() => normalizeStats(data.value.categoryStats))
const classifyCoverage = computed(() => {
  const total = Number(data.value.fieldCount || 0)
  const classified = normalizedLevelStats.value.reduce((sum, row) => sum + row.cnt, 0)
  return total ? Math.round(classified * 100 / total) : 0
})
const riskTone = computed(() => {
  if (Number(data.value.openRiskAlertCount || 0) > 0) return { type: 'danger', text: '存在待处理风险' }
  if (Number(data.value.pendingRequestCount || 0) > 0) return { type: 'warning', text: '存在待审批申请' }
  return { type: 'success', text: '治理状态平稳' }
})
async function load(){ data.value = await api.get('/dashboard/summary') }
async function autoClassify(){
  const res = await api.post('/field-classifications/auto-classify', {})
  ElMessage.success(`已自动分类 ${res.classified} 个字段`)
  load()
}
function normalizeStats(rows = []){
  const max = Math.max(...rows.map(row => Number(row.CNT || row.cnt || 0)), 1)
  return rows.map(row => {
    const cnt = Number(row.CNT || row.cnt || 0)
    return { name: row.NAME || row.name, cnt, percent: Math.max(6, Math.round(cnt * 100 / max)) }
  })
}
function levelText(code){
  return ({ L1: '公开', L2: '内部', L3: '敏感', L4: '高敏', L5: '核心' })[code] || '未定义'
}
onMounted(load)
</script>
