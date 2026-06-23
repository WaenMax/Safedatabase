<template>
  <div class="page-head">
    <div>
      <h1 class="page-title">字段资产管理</h1>
      <p>按数据源和数据表组织字段资产</p>
    </div>
    <el-button @click="$router.push('/agent/field-classify')">字段智能分类</el-button>
  </div>
  <div class="asset-workbench">
    <aside class="asset-tree">
      <div class="tree-head">
        <strong>资产目录</strong>
        <el-button text @click="clearTree">全部</el-button>
      </div>
      <el-tree :data="treeData" node-key="id" default-expand-all :expand-on-click-node="false" @node-click="selectTreeNode">
        <template #default="{ data }">
          <span class="tree-node">
            <span>{{ data.label }}</span>
            <el-tag size="small" :type="data.type === 'source' ? 'primary' : 'info'">{{ data.count }}</el-tag>
          </span>
        </template>
      </el-tree>
    </aside>
    <section class="asset-panel">
      <div class="toolbar">
        <el-input v-model="filters.source" clearable placeholder="按数据源筛选" style="width: 180px" />
        <el-input v-model="filters.table" clearable placeholder="按数据表筛选" style="width: 180px" />
        <el-input v-model="filters.field" clearable placeholder="按字段名筛选" style="width: 180px" />
        <el-select v-model="filters.sensitive" clearable placeholder="是否敏感" style="width: 140px">
          <el-option label="敏感字段" value="1" />
          <el-option label="非敏感字段" value="0" />
        </el-select>
        <el-button type="primary" @click="open({})">新增字段</el-button>
      </div>
      <div class="field-summary">
        <div><strong>{{ filteredRows.length }}</strong><span>当前字段</span></div>
        <div><strong>{{ sensitiveCount }}</strong><span>敏感字段</span></div>
        <div><strong>{{ highCount }}</strong><span>L4/L5 字段</span></div>
      </div>
      <el-table :data="filteredRows" border>
        <el-table-column prop="ID" label="ID" width="70" /><el-table-column prop="SOURCE_NAME" label="数据源" /><el-table-column prop="TABLE_NAME" label="表" /><el-table-column prop="FIELD_NAME" label="字段名" /><el-table-column prop="FIELD_TYPE" label="类型" /><el-table-column prop="FIELD_COMMENT" label="说明" /><el-table-column prop="CATEGORY_NAME" label="分类" /><el-table-column label="分级" width="90"><template #default="{row}"><el-tag :type="levelTag(row.LEVEL_CODE)">{{ row.LEVEL_CODE || '-' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="330"><template #default="{row}"><el-button size="small" @click="detail(row)">详情/脱敏</el-button><el-button size="small" @click="requestAccess(row)">申请原始值</el-button><el-button size="small" @click="open(row)">编辑</el-button><el-button size="small" type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
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
    </section>
  </div>
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
    <template #footer>
      <el-button v-if="raw.requiresApproval" type="primary" @click="requestAccess(current)">提交访问申请</el-button>
      <el-button @click="$router.push('/agent/field-classify')">跳转智能分类</el-button>
    </template>
  </el-dialog>
  <el-dialog v-model="requestVisible" title="高敏字段访问申请" width="520px">
    <el-form :model="requestForm" label-width="100px">
      <el-form-item label="申请字段"><el-input v-model="requestForm.field_name" disabled /></el-form-item>
      <el-form-item label="申请原因"><el-input v-model="requestForm.reason" type="textarea" :rows="4" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="requestVisible=false">取消</el-button><el-button type="primary" @click="submitRequest">提交申请</el-button></template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api/http'
const rows = ref([]), visible = ref(false), detailVisible = ref(false), requestVisible = ref(false)
const form = reactive({}), current = ref({}), raw = ref({}), masked = ref({})
const filters = reactive({ source: '', table: '', field: '', sensitive: '' })
const requestForm = reactive({ field_id: null, field_name: '', reason: '业务处理需要查看该字段原始样例值' })
const page = ref(1), pageSize = ref(20), total = ref(0)
const treeData = computed(() => {
  const sources = new Map()
  for (const row of rows.value) {
    const source = row.SOURCE_NAME || '未归属数据源'
    const table = row.TABLE_NAME || '未归属数据表'
    if (!sources.has(source)) sources.set(source, new Map())
    const tables = sources.get(source)
    tables.set(table, (tables.get(table) || 0) + 1)
  }
  return [...sources.entries()].map(([source, tables]) => ({
    id: `source:${source}`,
    label: source,
    type: 'source',
    count: [...tables.values()].reduce((sum, n) => sum + n, 0),
    children: [...tables.entries()].map(([table, count]) => ({ id: `table:${source}:${table}`, label: table, source, type: 'table', count }))
  }))
})
const filteredRows = computed(() => rows.value.filter(row => {
  const sourceHit = !filters.source || String(row.SOURCE_NAME || '').toLowerCase().includes(filters.source.toLowerCase())
  const tableHit = !filters.table || String(row.TABLE_NAME || '').toLowerCase().includes(filters.table.toLowerCase())
  const fieldHit = !filters.field || String(row.FIELD_NAME || '').toLowerCase().includes(filters.field.toLowerCase())
  const sensitiveHit = filters.sensitive === '' || String(row.IS_SENSITIVE) === filters.sensitive || String(Number(!!row.IS_SENSITIVE)) === filters.sensitive
  return sourceHit && tableHit && fieldHit && sensitiveHit
}))
const sensitiveCount = computed(() => filteredRows.value.filter(row => !!row.IS_SENSITIVE).length)
const highCount = computed(() => filteredRows.value.filter(row => ['L4', 'L5'].includes(row.LEVEL_CODE)).length)
async function load(){
  const res = await api.get(`/fields?page=${page.value}&pageSize=${pageSize.value}`)
  rows.value = res.rows; total.value = res.total
}
function lower(row){ const obj={}; for(const k in row)obj[k.toLowerCase()]=row[k]; return obj }
function open(row){ Object.keys(form).forEach(k=>delete form[k]); Object.assign(form, lower(row)); form.is_sensitive=!!form.is_sensitive; visible.value=true }
async function save(){ form.id ? await api.put(`/fields/${form.id}`, form) : await api.post('/fields', form); visible.value=false; ElMessage.success('保存成功'); load() }
async function remove(row){ await ElMessageBox.confirm('确认删除字段？'); await api.del(`/fields/${row.ID}`); load() }
async function detail(row){ current.value = await api.get(`/fields/${row.ID}`); raw.value = await api.get(`/fields/${row.ID}/raw-value`); masked.value = await api.get(`/fields/${row.ID}/masked-value`); detailVisible.value=true }
function selectTreeNode(node){
  filters.source = node.type === 'source' ? node.label : node.source
  filters.table = node.type === 'table' ? node.label : ''
}
function clearTree(){ filters.source=''; filters.table='' }
function levelTag(level){ return level === 'L5' || level === 'L4' ? 'danger' : level === 'L3' ? 'warning' : 'success' }
function requestAccess(row){
  requestForm.field_id = row.ID
  requestForm.field_name = row.FIELD_NAME
  requestForm.reason = '业务处理需要查看该字段原始样例值'
  requestVisible.value = true
}
async function submitRequest(){
  await api.post('/access-requests', { field_id: requestForm.field_id, reason: requestForm.reason })
  requestVisible.value = false
  detailVisible.value = false
  ElMessage.success('访问申请已提交')
}
onMounted(load)
</script>
