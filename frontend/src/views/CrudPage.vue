<template>
  <h1 class="page-title">{{ cfg.title }}</h1>
  <div class="toolbar"><el-button type="primary" @click="open({})">新增</el-button></div>
  <el-table :data="rows" border>
    <el-table-column v-for="c in cfg.columns" :key="c.prop" :prop="c.prop" :label="c.label" min-width="120" />
    <el-table-column label="操作" width="150">
      <template #default="{ row }"><el-button size="small" @click="open(row)">编辑</el-button><el-button size="small" type="danger" @click="remove(row)">删除</el-button></template>
    </el-table-column>
  </el-table>
  <el-dialog v-model="visible" :title="form.id ? '编辑' : '新增'" width="520px">
    <el-form :model="form" label-width="110px">
      <el-form-item v-for="f in cfg.fields" :key="f" :label="label(f)"><el-input v-model="form[f]" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { api } from '../api/http'
const props = defineProps({ type: String })
const rows = ref([])
const visible = ref(false)
const form = reactive({})
const configs = {
  sources: { title: '数据源管理', url: '/data-sources', columns: [{prop:'ID',label:'ID'},{prop:'SOURCE_NAME',label:'名称'},{prop:'SOURCE_TYPE',label:'类型'},{prop:'HOST',label:'主机'},{prop:'PORT',label:'端口'},{prop:'DATABASE_NAME',label:'库名'},{prop:'DESCRIPTION',label:'描述'}], fields: ['source_name','source_type','host','port','database_name','description'] },
  tables: { title: '数据表管理', url: '/tables', columns: [{prop:'ID',label:'ID'},{prop:'SOURCE_NAME',label:'数据源'},{prop:'TABLE_NAME',label:'表名'},{prop:'BUSINESS_NAME',label:'业务名称'},{prop:'OWNER_DEPARTMENT',label:'所属部门'},{prop:'DESCRIPTION',label:'描述'}], fields: ['source_id','table_name','business_name','owner_department','description'] }
}
const cfg = computed(() => configs[props.type])
function label(key) { return ({ source_id:'数据源ID', source_name:'数据源名称', source_type:'类型', host:'主机', port:'端口', database_name:'库名', table_name:'表名', business_name:'业务名称', owner_department:'所属部门', description:'描述' })[key] || key }
async function load() { rows.value = await api.get(cfg.value.url) }
function open(row) { Object.keys(form).forEach(k => delete form[k]); Object.assign(form, lower(row)); visible.value = true }
async function save() { form.id ? await api.put(`${cfg.value.url}/${form.id}`, form) : await api.post(cfg.value.url, form); visible.value=false; ElMessage.success('保存成功'); load() }
async function remove(row) { await ElMessageBox.confirm('确认删除该记录？'); await api.del(`${cfg.value.url}/${row.ID}`); load() }
function lower(row) { const obj = {}; for (const k in row) obj[k.toLowerCase()] = row[k]; return obj }
watch(() => props.type, load)
onMounted(load)
</script>
