<template>
  <h1 class="page-title">风险告警</h1>
  <div class="toolbar">
    <el-select v-model="level" clearable placeholder="风险等级" style="width:160px" @change="load">
      <el-option label="high" value="high" /><el-option label="medium" value="medium" /><el-option label="low" value="low" />
    </el-select>
    <el-button type="primary" @click="analyze">Agent 风险分析</el-button>
  </div>
  <el-table :data="rows" border>
    <el-table-column prop="ALERT_ID" label="ID" width="80" />
    <el-table-column prop="RISK_TYPE" label="风险类型" />
    <el-table-column prop="RISK_LEVEL" label="等级" width="100" />
    <el-table-column prop="USER_ID" label="用户ID" width="90" />
    <el-table-column prop="DESCRIPTION" label="风险说明" />
    <el-table-column prop="SUGGESTION" label="处置建议" />
    <el-table-column prop="STATUS" label="状态" width="110" />
    <el-table-column label="操作" width="120"><template #default="{row}"><el-button size="small" @click="handle(row)">标记已处理</el-button></template></el-table-column>
  </el-table>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/http'
const rows=ref([]), level=ref('')
async function load(){ rows.value = await api.get(`/agent/risk-alerts${level.value ? '?level='+level.value : ''}`) }
async function analyze(){ const r = await api.post('/agent/analyze-audit-logs',{}); ElMessage.success(`生成 ${r.length} 条风险告警`); load() }
async function handle(row){ await api.put(`/agent/risk-alerts/${row.ALERT_ID}/handle`,{}); ElMessage.success('已处理'); load() }
onMounted(load)
</script>
