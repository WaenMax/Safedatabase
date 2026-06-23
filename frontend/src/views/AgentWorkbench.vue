<template>
  <h1 class="page-title">智能体工作台</h1>
  <div class="stat-grid">
    <div class="stat">今日 Agent 执行任务数<strong>{{ data.todayTaskCount }}</strong></div>
    <div class="stat">待处理风险告警数<strong>{{ data.openAlertCount }}</strong></div>
    <div class="stat">高风险告警数<strong>{{ data.highAlertCount }}</strong></div>
    <div class="stat">本地规则引擎<strong>ON</strong></div>
  </div>
  <el-row :gutter="16">
    <el-col :span="12">
      <el-card><template #header>最近 Agent 建议</template>
        <el-table :data="data.recentRecommendations" border>
          <el-table-column prop="RECOMMENDATION_TYPE" label="类型" />
          <el-table-column prop="TARGET_ID" label="对象ID" width="90" />
          <el-table-column prop="RECOMMENDATION_RESULT" label="建议" />
          <el-table-column prop="RISK_LEVEL" label="风险" />
        </el-table>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card><template #header>最近风险告警</template>
        <el-table :data="data.recentAlerts" border>
          <el-table-column prop="RISK_TYPE" label="类型" />
          <el-table-column prop="RISK_LEVEL" label="等级" />
          <el-table-column prop="STATUS" label="状态" />
          <el-table-column prop="DESCRIPTION" label="说明" />
        </el-table>
      </el-card>
    </el-col>
  </el-row>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api/http'
const data = ref({})
onMounted(async () => { data.value = await api.get('/agent/workbench') })
</script>
