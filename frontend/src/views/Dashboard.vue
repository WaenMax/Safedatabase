<template>
  <h1 class="page-title">首页统计看板</h1>
  <div class="stat-grid">
    <div class="stat">数据源数量<strong>{{ data.sourceCount }}</strong></div>
    <div class="stat">数据表数量<strong>{{ data.tableCount }}</strong></div>
    <div class="stat">字段数量<strong>{{ data.fieldCount }}</strong></div>
    <div class="stat">敏感字段数量<strong>{{ data.sensitiveFieldCount }}</strong></div>
  </div>
  <el-row :gutter="16">
    <el-col :span="12">
      <el-card><template #header>各等级字段数量</template><el-table :data="data.levelStats"><el-table-column prop="NAME" label="等级" /><el-table-column prop="CNT" label="数量" /></el-table></el-card>
    </el-col>
    <el-col :span="12">
      <el-card><template #header>各分类字段数量</template><el-table :data="data.categoryStats"><el-table-column prop="NAME" label="分类" /><el-table-column prop="CNT" label="数量" /></el-table></el-card>
    </el-col>
  </el-row>
  <el-card style="margin-top:16px"><template #header>最近审计日志</template><el-table :data="data.recentAuditLogs"><el-table-column prop="OPERATION_TYPE" label="操作" /><el-table-column prop="USERNAME" label="用户" /><el-table-column prop="RESULT" label="结果" /><el-table-column prop="DETAIL" label="详情" /></el-table></el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api/http'
const data = ref({})
onMounted(async () => { data.value = await api.get('/dashboard/summary') })
</script>
