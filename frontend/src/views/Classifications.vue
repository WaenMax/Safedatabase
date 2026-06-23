<template>
  <h1 class="page-title">字段分类分级</h1>
  <div class="toolbar">
    <el-button type="primary" @click="open({})">人工分类</el-button>
    <el-button type="success" @click="autoClassify">自动分类</el-button>
  </div>
  <el-table :data="rows" border>
    <el-table-column prop="FIELD_NAME" label="字段" /><el-table-column prop="CATEGORY_NAME" label="分类" /><el-table-column prop="LEVEL_CODE" label="等级" /><el-table-column prop="CLASSIFY_METHOD" label="方式" /><el-table-column prop="REMARK" label="备注" />
    <el-table-column label="操作" width="110"><template #default="{row}"><el-button size="small" @click="open(row)">编辑</el-button></template></el-table-column>
  </el-table>
  <el-dialog v-model="visible" title="分类分级" width="520px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="字段ID"><el-input v-model="form.field_id" /></el-form-item>
      <el-form-item label="分类"><el-select v-model="form.category_id" style="width:100%"><el-option v-for="c in categories" :key="c.ID" :label="c.CATEGORY_NAME" :value="c.ID" /></el-select></el-form-item>
      <el-form-item label="分级"><el-select v-model="form.level_id" style="width:100%"><el-option v-for="l in levels" :key="l.ID" :label="`${l.LEVEL_CODE} ${l.LEVEL_NAME}`" :value="l.ID" /></el-select></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/http'
const rows=ref([]), categories=ref([]), levels=ref([]), visible=ref(false), form=reactive({})
async function load(){ rows.value=await api.get('/field-classifications'); categories.value=await api.get('/categories'); levels.value=await api.get('/levels') }
function open(row){ Object.keys(form).forEach(k=>delete form[k]); Object.assign(form,{ id:row.ID, field_id:row.FIELD_ID, category_id:row.CATEGORY_ID, level_id:row.LEVEL_ID, remark:row.REMARK }); visible.value=true }
async function save(){ form.id ? await api.put(`/field-classifications/${form.id}`, form) : await api.post('/field-classifications', form); visible.value=false; ElMessage.success('保存成功'); load() }
async function autoClassify(){ const r=await api.post('/field-classifications/auto-classify',{}); ElMessage.success(`自动分类完成：${r.classified} 个字段`); load() }
onMounted(load)
</script>
