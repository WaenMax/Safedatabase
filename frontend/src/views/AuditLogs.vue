<template>
  <h1 class="page-title">审计日志</h1>
  <el-table :data="rows" border>
    <el-table-column prop="OPERATION_TIME" label="时间" width="190" /><el-table-column prop="USERNAME" label="用户" /><el-table-column prop="OPERATION_TYPE" label="操作类型" /><el-table-column prop="TARGET_TYPE" label="对象类型" /><el-table-column prop="TARGET_ID" label="对象ID" /><el-table-column prop="RESULT" label="结果" /><el-table-column prop="DETAIL" label="详情" min-width="220" />
  </el-table>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api/http'
const rows=ref([])
onMounted(async()=>{ rows.value=await api.get('/audit-logs') })
</script>
