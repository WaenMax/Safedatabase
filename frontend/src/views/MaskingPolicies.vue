<template>
  <h1 class="page-title">脱敏策略展示</h1>
  <el-table :data="rows" border>
    <el-table-column prop="POLICY_NAME" label="策略名称" /><el-table-column prop="POLICY_TYPE" label="类型" /><el-table-column prop="EXAMPLE_BEFORE" label="脱敏前" /><el-table-column prop="EXAMPLE_AFTER" label="脱敏后" /><el-table-column prop="DESCRIPTION" label="说明" />
  </el-table>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api/http'
const rows=ref([])
onMounted(async()=>{ rows.value=await api.get('/masking-policies') })
</script>
