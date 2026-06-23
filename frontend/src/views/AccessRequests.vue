<template>
  <h1 class="page-title">{{ mode === 'approver' ? '审批管理' : '数据访问申请' }}</h1>
  <div class="toolbar" v-if="mode !== 'approver'"><el-button type="primary" @click="visible=true">提交申请</el-button></div>
  <el-table :data="rows" border>
    <el-table-column prop="ID" label="ID" width="70" /><el-table-column prop="USERNAME" label="申请人" /><el-table-column prop="FIELD_NAME" label="字段" /><el-table-column prop="LEVEL_CODE" label="等级" /><el-table-column prop="REASON" label="申请原因" /><el-table-column prop="STATUS" label="状态" /><el-table-column prop="VALID_UNTIL" label="有效期" />
    <el-table-column v-if="mode === 'approver'" label="审批" width="180"><template #default="{row}"><el-button size="small" type="success" @click="approve(row)">通过</el-button><el-button size="small" type="danger" @click="reject(row)">驳回</el-button></template></el-table-column>
  </el-table>
  <el-dialog v-model="visible" title="提交访问申请" width="500px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="字段ID"><el-input v-model="form.field_id" /></el-form-item>
      <el-form-item label="申请原因"><el-input v-model="form.reason" type="textarea" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="submit">提交</el-button></template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/http'
const props=defineProps({ mode:String })
const rows=ref([]), visible=ref(false), form=reactive({ field_id:4, reason:'业务排查需要查看原始样例值' })
async function load(){ rows.value=await api.get('/access-requests') }
async function submit(){ await api.post('/access-requests', form); visible.value=false; ElMessage.success('申请已提交'); load() }
async function approve(row){ await api.put(`/access-requests/${row.ID}/approve`,{}); ElMessage.success('已通过'); load() }
async function reject(row){ await api.put(`/access-requests/${row.ID}/reject`,{}); ElMessage.success('已驳回'); load() }
watch(()=>props.mode, load)
onMounted(load)
</script>
