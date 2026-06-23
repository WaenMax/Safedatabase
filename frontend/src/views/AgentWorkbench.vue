<template>
  <div class="page-head">
    <div>
      <h1 class="page-title">智能体工作台</h1>
      <p>建议、告警、报告与问答任务态势</p>
    </div>
    <el-button type="primary" @click="$router.push('/agent/chat')">进入独立问答</el-button>
  </div>

  <div class="agent-console">
    <div class="agent-radar">
      <div class="radar-ring">
        <strong>{{ data.highAlertCount || 0 }}</strong>
        <span>高风险</span>
      </div>
      <p>规则引擎在线，外部模型不可用时自动回退本地推理。</p>
    </div>
    <div class="agent-actions">
      <button @click="$router.push('/agent/field-classify')">字段智能分类</button>
      <button @click="$router.push('/agent/approval-advice')">审批建议</button>
      <button @click="$router.push('/agent/risk-alerts')">风险告警分析</button>
      <button @click="$router.push('/agent/security-report')">生成安全报告</button>
    </div>
  </div>

  <div class="stat-grid">
    <div class="stat accent-blue">今日 Agent 执行任务数<strong>{{ data.todayTaskCount }}</strong><span>任务与问答均留痕</span></div>
    <div class="stat accent-orange">待处理风险告警数<strong>{{ data.openAlertCount }}</strong><span>等待安全管理员确认</span></div>
    <div class="stat accent-red">高风险告警数<strong>{{ data.highAlertCount }}</strong><span>优先复核访问行为</span></div>
    <div class="stat accent-green">本地规则引擎<strong>ON</strong><span>离线可用的安全基线</span></div>
  </div>
  <el-row :gutter="16">
    <el-col :span="12">
      <el-card><template #header>最近 Agent 建议</template>
        <div class="agent-feed">
          <div v-for="item in data.recentRecommendations" :key="item.RECOMMENDATION_ID" class="feed-item">
            <el-tag size="small">{{ item.RECOMMENDATION_TYPE }}</el-tag>
            <strong>{{ item.RECOMMENDATION_RESULT }}</strong>
            <p>{{ item.REASON }}</p>
            <span>对象 {{ item.TARGET_TYPE }} #{{ item.TARGET_ID }} · 置信度 {{ item.CONFIDENCE }}</span>
          </div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card><template #header>最近风险告警</template>
        <div class="alert-stack">
          <div v-for="item in data.recentAlerts" :key="item.ALERT_ID" class="alert-card" :class="`risk-${item.RISK_LEVEL}`">
            <div><strong>{{ item.RISK_TYPE }}</strong><el-tag size="small" :type="tagType(item.RISK_LEVEL)">{{ item.RISK_LEVEL }}</el-tag></div>
            <p>{{ item.DESCRIPTION }}</p>
            <span>{{ item.STATUS }} · {{ item.SUGGESTION }}</span>
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api/http'
const data = ref({})
function tagType(level){
  return level === 'high' ? 'danger' : level === 'medium' ? 'warning' : 'success'
}
onMounted(async () => { data.value = await api.get('/agent/workbench') })
</script>
