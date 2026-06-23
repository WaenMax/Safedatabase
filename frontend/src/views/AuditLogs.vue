<template>
  <div class="page-head">
    <div>
      <h1 class="page-title">审计日志</h1>
      <p>关键访问、审批、分类和 Agent 操作时间线，支持多维度筛选</p>
    </div>
    <span class="audit-count">共 {{ rows.length }} 条</span>
  </div>
  <div class="toolbar">
    <el-input v-model="filters.user" clearable placeholder="按用户筛选" style="width: 150px" @change="load" />
    <el-input v-model="filters.operation" clearable placeholder="按操作类型筛选" style="width: 180px" @change="load" />
    <el-select v-model="filters.result" clearable placeholder="结果" style="width: 130px" @change="load">
      <el-option label="SUCCESS" value="SUCCESS" />
      <el-option label="DENIED" value="DENIED" />
      <el-option label="FAIL" value="FAIL" />
    </el-select>
    <el-date-picker
      v-model="filters.dateRange"
      type="datetimerange"
      range-separator="至"
      start-placeholder="开始时间"
      end-placeholder="结束时间"
      format="YYYY-MM-DD HH:mm"
      value-format="YYYY-MM-DD HH:mm:ss"
      style="width: 340px"
      @change="load"
    />
    <el-button @click="clearFilters">清除筛选</el-button>
  </div>
  <div class="audit-layout">
    <el-timeline v-if="rows.length">
      <el-timeline-item v-for="row in rows" :key="row.ID" :type="timelineType(row)" :timestamp="row.OPERATION_TIME">
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
    <el-empty v-if="rows.length === 0" description="暂无匹配的审计日志" />
  </div>
  <el-pagination
    v-if="total > pageSize"
    style="margin-top: 16px; justify-content: center"
    background
    layout="total, prev, pager, next, sizes"
    :total="total"
    v-model:current-page="page"
    v-model:page-size="pageSize"
    :page-sizes="[10, 20, 50, 100]"
    @current-change="load"
    @size-change="load"
  />
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '../api/http'

const rows = ref([])
const filters = reactive({ user: '', result: '', operation: '', dateRange: null })

function buildParams() {
  const params = {}
  if (filters.user) params.user = filters.user
  if (filters.result) params.result = filters.result
  if (filters.operation) params.operation = filters.operation
  if (filters.dateRange && filters.dateRange.length === 2) {
    params.startTime = filters.dateRange[0]
    params.endTime = filters.dateRange[1]
  }
  return params
}

const page = ref(1), pageSize = ref(20), total = ref(0)
async function load() {
  const params = buildParams()
  params.page = page.value
  params.pageSize = pageSize.value
  const query = Object.entries(params).map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&')
  const res = await api.get(`/audit-logs?${query}`)
  rows.value = res.rows; total.value = res.total
}

function clearFilters() {
  filters.user = ''
  filters.operation = ''
  filters.result = ''
  filters.dateRange = null
  load()
}

function isRisk(row) {
  return ['DENIED', 'FAIL'].includes(row.RESULT) ||
    String(row.OPERATION_TYPE || '').includes('RAW') ||
    String(row.OPERATION_TYPE || '').includes('AGENT')
}
function resultTag(result) {
  return result === 'SUCCESS' ? 'success' : result === 'DENIED' ? 'warning' : 'danger'
}
function timelineType(row) {
  return row.RESULT === 'SUCCESS' ? 'success' : row.RESULT === 'DENIED' ? 'warning' : 'danger'
}

onMounted(load)
</script>
