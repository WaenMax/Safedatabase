<template>
  <h1 class="page-title">分类规则管理</h1>
  <div class="toolbar"><el-button type="primary" @click="open({enabled:true,match_type:'keyword'})">新增规则</el-button></div>
  <el-table :data="rows" border>
    <el-table-column prop="RULE_NAME" label="规则名" /><el-table-column prop="MATCH_TYPE" label="匹配类型" /><el-table-column prop="MATCH_PATTERN" label="匹配内容" /><el-table-column prop="CATEGORY_NAME" label="分类" /><el-table-column prop="LEVEL_CODE" label="等级" /><el-table-column prop="ENABLED" label="启用" />
    <el-table-column label="操作" width="150"><template #default="{row}"><el-button size="small" @click="open(row)">编辑</el-button><el-button size="small" type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
  </el-table>
  <el-dialog v-model="visible" title="规则维护" width="560px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="规则名"><el-input v-model="form.rule_name" /></el-form-item>
      <el-form-item label="匹配类型"><el-select v-model="form.match_type" style="width:100%"><el-option label="keyword" value="keyword" /><el-option label="regex" value="regex" /></el-select></el-form-item>
      <el-form-item label="匹配内容"><el-input v-model="form.match_pattern" /></el-form-item>
      <el-form-item label="分类ID"><el-input v-model="form.category_id" /></el-form-item>
      <el-form-item label="等级ID"><el-input v-model="form.level_id" /></el-form-item>
      <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api/http'
const rows=ref([]), visible=ref(false), form=reactive({})
async function load(){ rows.value=await api.get('/rules') }
function open(row){ Object.keys(form).forEach(k=>delete form[k]); for(const k in row) form[k.toLowerCase()]=row[k]; form.enabled=!!form.enabled; visible.value=true }
async function save(){ form.id ? await api.put(`/rules/${form.id}`, form) : await api.post('/rules', form); visible.value=false; ElMessage.success('保存成功'); load() }
async function remove(row){ await ElMessageBox.confirm('确认删除规则？'); await api.del(`/rules/${row.ID}`); load() }
onMounted(load)
</script>
