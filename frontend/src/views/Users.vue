<template>
  <div class="page-head">
    <div>
      <h1 class="page-title">用户管理</h1>
      <p>演示角色与账号维护，角色绑定权限控制</p>
    </div>
  </div>
  <div class="toolbar"><el-button type="primary" @click="open({enabled:true,password:'123456'})">新增用户</el-button></div>
  <el-table :data="rows" border>
    <el-table-column prop="ID" label="ID" width="70" /><el-table-column prop="USERNAME" label="用户名" /><el-table-column prop="REAL_NAME" label="姓名" /><el-table-column prop="EMAIL" label="邮箱" /><el-table-column prop="PHONE" label="手机号" /><el-table-column prop="ROLE_CODE" label="角色" /><el-table-column prop="ENABLED" label="启用" />
    <el-table-column label="操作" width="150"><template #default="{ row }"><el-button size="small" @click="open(row)">编辑</el-button><el-button size="small" type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
  </el-table>
  <el-pagination
    v-if="total > pageSize"
    style="margin-top: 16px; justify-content: flex-end"
    background
    layout="total, prev, pager, next, sizes"
    :total="total"
    v-model:current-page="page"
    v-model:page-size="pageSize"
    :page-sizes="[10, 20, 50, 100]"
    @current-change="load"
    @size-change="load"
  />
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
const page=ref(1), pageSize=ref(20), total=ref(0)
async function load(){
  const res=await api.get(`/users?page=${page.value}&pageSize=${pageSize.value}`)
  rows.value=res.rows; total.value=res.total
  roles.value=await api.get('/roles')
}
function open(row){ Object.keys(form).forEach(k=>delete form[k]); for(const k in row) form[k.toLowerCase()]=row[k]; form.enabled=!!form.enabled; if(!form.role_id) form.role_id=3; visible.value=true }
async function save(){ form.id ? await api.put(`/users/${form.id}`, form) : await api.post('/users', form); visible.value=false; ElMessage.success('保存成功'); load() }
async function remove(row){ await ElMessageBox.confirm('确认删除用户？'); await api.del(`/users/${row.ID}`); load() }
onMounted(load)
</script>
