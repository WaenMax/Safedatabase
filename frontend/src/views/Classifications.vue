<template>
  <div class="page-head">
    <div>
      <h1 class="page-title">字段分类分级</h1>
      <p>分类依据、等级解释和 Agent 建议证据链</p>
    </div>
  </div>
  <div class="level-matrix">
    <div v-for="item in levelMatrix" :key="item.code" :class="`matrix-${item.code}`">
      <strong>{{ item.code }}</strong>
      <span>{{ item.name }}</span>
      <b>{{ item.count }}</b>
    </div>
  </div>
  <div class="toolbar">
    <el-input v-model="filters.field" clearable placeholder="字段名" style="width: 180px" />
    <el-select v-model="filters.level" clearable placeholder="等级" style="width: 130px">
      <el-option v-for="l in levels" :key="l.ID" :label="l.LEVEL_CODE" :value="l.LEVEL_CODE" />
    </el-select>
    <el-select v-model="filters.method" clearable placeholder="分类方式" style="width: 150px">
      <el-option label="MANUAL" value="MANUAL" />
      <el-option label="AUTO" value="AUTO" />
      <el-option label="AGENT" value="AGENT" />
    </el-select>
    <el-button type="primary" @click="open({})">人工分类</el-button>
    <el-button type="success" @click="autoClassify">规则自动分类</el-button>
    <el-button @click="agentBatch(false)">批量 Agent 分析</el-button>
    <el-button type="warning" @click="agentBatch(true)">批量应用 Agent</el-button>
  </div>
  <el-table :data="filteredRows" border>
    <el-table-column type="expand">
      <template #default="{ row }">
        <div class="evidence-box">
          <div><strong>规则证据</strong><span>{{ row.EVIDENCE_RULE }}</span></div>
          <div><strong>格式证据</strong><span>{{ row.EVIDENCE_FORMAT }}</span></div>
          <div><strong>等级解释</strong><span>{{ row.LEVEL_EXPLANATION }}</span></div>
          <div><strong>分类理由</strong><span>{{ row.REMARK || '-' }}</span></div>
        </div>
      </template>
    </el-table-column>
    <el-table-column prop="FIELD_NAME" label="字段" />
    <el-table-column prop="CATEGORY_NAME" label="分类" />
    <el-table-column label="等级" width="90"><template #default="{ row }"><el-tag :type="levelTag(row.LEVEL_CODE)">{{ row.LEVEL_CODE }}</el-tag></template></el-table-column>
    <el-table-column prop="CLASSIFY_METHOD" label="方式" width="110" />
    <el-table-column prop="CLASSIFIED_TIME" label="更新时间" width="180" />
    <el-table-column prop="EVIDENCE_RULE" label="命中规则" min-width="180" />
    <el-table-column label="操作" width="170"><template #default="{ row }"><el-button size="small" @click="agentOne(row)">Agent</el-button><el-button size="small" @click="open(row)">编辑</el-button></template></el-table-column>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/http'
const rows=ref([]), categories=ref([]), levels=ref([]), visible=ref(false), form=reactive({})
const filters=reactive({ field:'', level:'', method:'' })
const page=ref(1), pageSize=ref(20), total=ref(0)
const filteredRows=computed(()=>rows.value.filter(row=>
  (!filters.field || String(row.FIELD_NAME).toLowerCase().includes(filters.field.toLowerCase())) &&
  (!filters.level || row.LEVEL_CODE === filters.level) &&
  (!filters.method || row.CLASSIFY_METHOD === filters.method)
))
const levelMatrix=computed(()=>levels.value.map(level=>({
  code: level.LEVEL_CODE,
  name: level.LEVEL_NAME,
  count: rows.value.filter(row=>row.LEVEL_CODE===level.LEVEL_CODE).length
})))
async function load(){
  const res=await api.get(`/field-classifications?page=${page.value}&pageSize=${pageSize.value}`)
  rows.value=res.rows; total.value=res.total
  categories.value=await api.get('/categories'); levels.value=await api.get('/levels')
}
function open(row){ Object.keys(form).forEach(k=>delete form[k]); Object.assign(form,{ id:row.ID, field_id:row.FIELD_ID, category_id:row.CATEGORY_ID, level_id:row.LEVEL_ID, remark:row.REMARK }); visible.value=true }
async function save(){ form.id ? await api.put(`/field-classifications/${form.id}`, form) : await api.post('/field-classifications', form); visible.value=false; ElMessage.success('保存成功'); load() }
async function autoClassify(){ const r=await api.post('/field-classifications/auto-classify',{}); ElMessage.success(`自动分类完成：${r.classified} 个字段`); load() }
async function agentOne(row){ const r=await api.post(`/agent/classify-field/${row.FIELD_ID}?apply=false`,{}); ElMessage.success(`${r.levelCode} ${r.reason}`) }
async function agentBatch(apply){ const r=await api.post(`/agent/classify-all-fields?apply=${apply}`,{}); ElMessage.success(`${apply ? '已应用' : '已分析'} ${r.length} 个字段`); load() }
function levelTag(level){ return level === 'L5' || level === 'L4' ? 'danger' : level === 'L3' ? 'warning' : 'success' }
onMounted(load)
</script>
