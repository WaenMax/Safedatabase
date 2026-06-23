<template>
  <h1 class="page-title">字段智能分类</h1>
  <div class="toolbar">
    <el-button type="primary" @click="load">刷新未分类字段</el-button>
    <el-button type="success" @click="classifyAll">Agent 分析全部字段</el-button>
  </div>
  <el-alert v-if="rows.length===0" title="暂无未分类字段。可以先在字段资产管理中新增 id_card_no，再回到本页演示 Agent 分析。" type="info" show-icon />
  <el-table :data="rows" border style="margin-top:12px">
    <el-table-column prop="ID" label="字段ID" width="90" />
    <el-table-column prop="TABLE_NAME" label="表" />
    <el-table-column prop="FIELD_NAME" label="字段名" />
    <el-table-column prop="FIELD_TYPE" label="类型" />
    <el-table-column prop="SAMPLE_VALUE" label="样例值" />
    <el-table-column label="操作" width="220"><template #default="{row}">
      <el-button size="small" @click="analyze(row, false)">Agent 分析</el-button>
      <el-button size="small" type="success" @click="analyze(row, true)">应用建议</el-button>
    </template></el-table-column>
  </el-table>
  <el-card v-if="result" style="margin-top:16px">
    <template #header>Agent 分类建议</template>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="字段ID">{{ result.fieldId }}</el-descriptions-item>
      <el-descriptions-item label="建议分类">{{ result.categoryName }}</el-descriptions-item>
      <el-descriptions-item label="建议分级">{{ result.levelCode }} {{ result.levelName }}</el-descriptions-item>
      <el-descriptions-item label="置信度">{{ result.confidence }}</el-descriptions-item>
      <el-descriptions-item label="理由">{{ result.reason }}</el-descriptions-item>
      <el-descriptions-item label="处置建议">{{ result.suggestion }}</el-descriptions-item>
      <el-descriptions-item label="已应用">{{ result.applied ? '是' : '否' }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/http'
const rows = ref([]), result = ref(null)
async function load(){ rows.value = await api.get('/agent/unclassified-fields') }
async function analyze(row, apply){ result.value = await api.post(`/agent/classify-field/${row.ID}?apply=${apply}`, {}); if(apply){ ElMessage.success('建议已应用'); load() } }
async function classifyAll(){ const r = await api.post('/agent/classify-all-fields?apply=false', {}); ElMessage.success(`已分析 ${r.length} 个字段`); result.value = r[0] }
onMounted(load)
</script>
