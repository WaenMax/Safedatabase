<template>
  <h1 class="page-title">用户管理</h1>
  <div class="toolbar"><el-button type="primary" @click="open({enabled:true,password:'123456'})">新增用户</el-button></div>
  <el-table :data="rows" border>
    <el-table-column prop="ID" label="ID" width="70" /><el-table-column prop="USERNAME" label="用户名" /><el-table-column prop="REAL_NAME" label="姓名" /><el-table-column prop="EMAIL" label="邮箱" /><el-table-column prop="PHONE" label="手机号" /><el-table-column prop="ROLE_CODE" label="角色" /><el-table-column prop="ENABLED" label="启用" />
    <el-table-column label="操作" width="150"><template #default="{row}"><el-button size="small" @click="open(row)">编辑</el-button><el-button size="small" type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
  </el-table>
  <el-dialog v-model="visible" title="用户维护" width="520px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
      <el-form-item v-if="!form.id" label="初始密码"><el-input v-model="form.password" /></el-form-item>
      <el-form-item label="姓名"><el-input v-model="form.real_name" /></el-form-item>
      <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
      <el-form-item label="角色"><el-select v-model="form.role_id" style="width:100%"><el-option v-for="r in roles" :key="r.ID" :label="r.ROLE_NAME" :value="r.ID" /></el-select></el-form-item>
      <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
  </el-dialog>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api/http'
const rows=ref([]), roles=ref([]), visible=ref(false), form=reactive({})
async function load(){ rows.value=await api.get('/users'); roles.value=await api.get('/roles') }
function open(row){ Object.keys(form).forEach(k=>delete form[k]); for(const k in row) form[k.toLowerCase()]=row[k]; form.enabled=!!form.enabled; if(!form.role_id) form.role_id=3; visible.value=true }
async function save(){ form.id ? await api.put(`/users/${form.id}`, form) : await api.post('/users', form); visible.value=false; ElMessage.success('保存成功'); load() }
async function remove(row){ await ElMessageBox.confirm('确认删除用户？'); await api.del(`/users/${row.ID}`); load() }
onMounted(load)
</script>
