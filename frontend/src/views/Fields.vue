<template>
  <h1 class="page-title">字段资产管理</h1>
  <div class="toolbar"><el-button type="primary" @click="open({})">新增字段</el-button></div>
  <el-table :data="rows" border>
    <el-table-column prop="ID" label="ID" width="70" /><el-table-column prop="TABLE_NAME" label="表" /><el-table-column prop="FIELD_NAME" label="字段名" /><el-table-column prop="FIELD_TYPE" label="类型" /><el-table-column prop="FIELD_COMMENT" label="说明" /><el-table-column prop="CATEGORY_NAME" label="分类" /><el-table-column prop="LEVEL_CODE" label="分级" /><el-table-column prop="IS_SENSITIVE" label="敏感" />
    <el-table-column label="操作" width="250"><template #default="{row}"><el-button size="small" @click="detail(row)">详情</el-button><el-button size="small" @click="open(row)">编辑</el-button><el-button size="small" type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
  </el-table>
  <el-dialog v-model="visible" title="字段维护" width="560px">
    <el-form :model="form" label-width="110px">
      <el-form-item label="表ID"><el-input v-model="form.table_id" /></el-form-item>
      <el-form-item label="字段名"><el-input v-model="form.field_name" /></el-form-item>
      <el-form-item label="字段类型"><el-input v-model="form.field_type" /></el-form-item>
      <el-form-item label="字段说明"><el-input v-model="form.field_comment" /></el-form-item>
      <el-form-item label="样例值"><el-input v-model="form.sample_value" /></el-form-item>
      <el-form-item label="是否敏感"><el-switch v-model="form.is_sensitive" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
  </el-dialog>
  <el-dialog v-model="detailVisible" title="字段详情" width="620px">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="字段名">{{ current.FIELD_NAME }}</el-descriptions-item>
      <el-descriptions-item label="字段类型">{{ current.FIELD_TYPE }}</el-descriptions-item>
      <el-descriptions-item label="原始样例值">{{ raw.value }}</el-descriptions-item>
      <el-descriptions-item label="脱敏样例值">{{ masked.value }}</el-descriptions-item>
      <el-descriptions-item label="分类">{{ current.CATEGORY_NAME }}</el-descriptions-item>
      <el-descriptions-item label="分级">{{ current.LEVEL_CODE }} {{ current.LEVEL_NAME }}</el-descriptions-item>
      <el-descriptions-item label="是否需要审批">{{ raw.requiresApproval ? '是' : '否' }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api/http'
const rows = ref([]), visible = ref(false), detailVisible = ref(false)
const form = reactive({}), current = ref({}), raw = ref({}), masked = ref({})
async function load(){ rows.value = await api.get('/fields') }
function lower(row){ const obj={}; for(const k in row)obj[k.toLowerCase()]=row[k]; return obj }
function open(row){ Object.keys(form).forEach(k=>delete form[k]); Object.assign(form, lower(row)); form.is_sensitive=!!form.is_sensitive; visible.value=true }
async function save(){ form.id ? await api.put(`/fields/${form.id}`, form) : await api.post('/fields', form); visible.value=false; ElMessage.success('保存成功'); load() }
async function remove(row){ await ElMessageBox.confirm('确认删除字段？'); await api.del(`/fields/${row.ID}`); load() }
async function detail(row){ current.value = await api.get(`/fields/${row.ID}`); raw.value = await api.get(`/fields/${row.ID}/raw-value`); masked.value = await api.get(`/fields/${row.ID}/masked-value`); detailVisible.value=true }
onMounted(load)
</script>
