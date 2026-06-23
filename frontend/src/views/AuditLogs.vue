<template>
  <div class="page-head">
    <div>
      <h1 class="page-title">审计日志</h1>
      <p>关键访问、审批、分类和 Agent 操作时间线</p>
    </div>
  </div>
  <div class="toolbar">
    <el-input v-model="filters.user" clearable placeholder="用户" style="width:160px" />
    <el-select v-model="filters.result" clearable placeholder="结果" style="width:130px">
      <el-option label="SUCCESS" value="SUCCESS" />
      <el-option label="DENIED" value="DENIED" />
      <el-option label="FAIL" value="FAIL" />
    </el-select>
    <el-input v-model="filters.operation" clearable placeholder="操作类型" style="width:180px" />
  </div>
  <div class="audit-layout">
    <el-timeline>
      <el-timeline-item v-for="row in filteredRows" :key="row.ID" :type="timelineType(row)" :timestamp="row.OPERATION_TIME">
        <div class="audit-card">
          <div>
            <strong>{{ row.OPERATION_TYPE }}</strong>
            <el-tag size="small" :type="resultTag(row.RESULT)">{{ row.RESULT }}</el-tag>
            <el-tag v-if="isRisk(row)" size="small" type="danger">风险相关</el-tag>
          </div>
          <p>{{ row.DETAIL }}</p>
          <span>{{ row.USERNAME || 'system' }} · {{ row.TARGET_TYPE || '-' }} #{{ row.TARGET_ID || '-' }} · {{ row.IP_ADDRESS }}</span>
        </div>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-if="filteredRows.length===0" description="暂无审计日志" />
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../api/http'
const rows=ref([])
const filters=reactive({ user:'', result:'', operation:'' })
const filteredRows=computed(()=>rows.value.filter(row=>
  (!filters.user || String(row.USERNAME || '').toLowerCase().includes(filters.user.toLowerCase())) &&
  (!filters.result || row.RESULT === filters.result) &&
  (!filters.operation || String(row.OPERATION_TYPE || '').toLowerCase().includes(filters.operation.toLowerCase()))
))
function isRisk(row){ return ['DENIED','FAIL'].includes(row.RESULT) || String(row.OPERATION_TYPE || '').includes('RAW') || String(row.OPERATION_TYPE || '').includes('AGENT') }
function resultTag(result){ return result === 'SUCCESS' ? 'success' : result === 'DENIED' ? 'warning' : 'danger' }
function timelineType(row){ return row.RESULT === 'SUCCESS' ? 'success' : row.RESULT === 'DENIED' ? 'warning' : 'danger' }
onMounted(async()=>{ rows.value=await api.get('/audit-logs') })
</script>
