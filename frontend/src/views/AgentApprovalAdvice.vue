<template>
  <h1 class="page-title">审批建议</h1>
  <el-table :data="rows" border>
    <el-table-column prop="ID" label="申请ID" width="90" />
    <el-table-column prop="USERNAME" label="申请人" />
    <el-table-column prop="FIELD_NAME" label="字段" />
    <el-table-column prop="LEVEL_CODE" label="等级" />
    <el-table-column prop="REASON" label="理由" />
    <el-table-column prop="STATUS" label="状态" />
    <el-table-column label="Agent/审批" width="260"><template #default="{row}">
      <el-button size="small" @click="review(row)">Agent 审查</el-button>
      <el-button size="small" type="success" @click="approve(row)">通过</el-button>
      <el-button size="small" type="danger" @click="reject(row)">驳回</el-button>
    </template></el-table-column>
  </el-table>
  <el-card v-if="advice" style="margin-top:16px">
    <template #header>Agent 审批建议</template>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="建议结果">{{ advice.recommendation }}</el-descriptions-item>
      <el-descriptions-item label="风险等级">{{ advice.riskLevel }}</el-descriptions-item>
      <el-descriptions-item label="置信度">{{ advice.confidence }}</el-descriptions-item>
      <el-descriptions-item label="理由">{{ advice.reason }}</el-descriptions-item>
      <el-descriptions-item label="处置建议">{{ advice.suggestion }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/http'
const rows=ref([]), advice=ref(null)
async function load(){ rows.value = await api.get('/access-requests') }
async function review(row){ advice.value = await api.post(`/agent/review-access-request/${row.ID}`, {}) }
async function approve(row){ await api.put(`/access-requests/${row.ID}/approve`,{}); ElMessage.success('已通过'); load() }
async function reject(row){ await api.put(`/access-requests/${row.ID}/reject`,{}); ElMessage.success('已驳回'); load() }
onMounted(load)
</script>
