<template>
  <div class="page-head">
    <div>
      <h1 class="page-title">{{ mode === 'approver' ? '审批管理' : '数据访问申请' }}</h1>
      <p>{{ mode === 'approver' ? '高敏访问审批与 Agent 辅助判断' : '高敏字段原始值访问申请记录' }}</p>
    </div>
  </div>
  <div class="approval-summary">
    <div><strong>{{ pendingCount }}</strong><span>待审批</span></div>
    <div><strong>{{ highCount }}</strong><span>L4/L5 申请</span></div>
    <div><strong>{{ approvedCount }}</strong><span>已通过</span></div>
    <div><strong>{{ rejectedCount }}</strong><span>已驳回</span></div>
  </div>
  <div class="toolbar" v-if="mode !== 'approver'"><el-button type="primary" @click="visible=true">提交申请</el-button></div>
  <div class="approval-workbench">
    <el-table :data="rows" border highlight-current-row @current-change="selectRow">
      <el-table-column prop="ID" label="ID" width="70" /><el-table-column prop="USERNAME" label="申请人" /><el-table-column prop="FIELD_NAME" label="字段" /><el-table-column label="等级" width="90"><template #default="{row}"><el-tag :type="levelTag(row.LEVEL_CODE)">{{ row.LEVEL_CODE }}</el-tag></template></el-table-column><el-table-column prop="REASON" label="申请原因" /><el-table-column label="状态" width="110"><template #default="{row}"><el-tag :type="statusTag(row.STATUS)">{{ row.STATUS }}</el-tag></template></el-table-column><el-table-column prop="VALID_UNTIL" label="有效期" />
      <el-table-column v-if="mode === 'approver'" label="审批" width="250"><template #default="{row}"><el-button size="small" @click.stop="review(row)">Agent 建议</el-button><el-button size="small" type="success" @click.stop="approve(row)">通过</el-button><el-button size="small" type="danger" @click.stop="reject(row)">驳回</el-button></template></el-table-column>
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
    <aside v-if="mode === 'approver'" class="approval-side">
      <template v-if="selected">
        <strong>申请 #{{ selected.ID }}</strong>
        <p>{{ selected.USERNAME }} 申请查看 {{ selected.FIELD_NAME }}，等级 {{ selected.LEVEL_CODE }}。</p>
        <p class="muted">{{ selected.REASON }}</p>
        <div v-if="detail" class="detail-stack">
          <div><strong>分类依据</strong><span>{{ detail.request?.CLASSIFY_REASON || '-' }}</span></div>
          <div><strong>脱敏策略</strong><span>{{ policyNames }}</span></div>
          <div><strong>历史访问</strong><span>{{ detail.historyAccess?.length || 0 }} 条</span></div>
          <div><strong>相关风险</strong><span>{{ detail.riskAlerts?.length || 0 }} 条</span></div>
        </div>
        <el-button type="primary" :loading="adviceLoading" @click="review(selected)">生成 Agent 建议</el-button>
        <div v-if="advice" class="advice-box">
          <el-tag :type="adviceTag(advice.recommendation)">{{ advice.recommendation }}</el-tag>
          <strong>风险：{{ advice.riskLevel }} · 置信度 {{ advice.confidence }}</strong>
          <p>{{ advice.reason }}</p>
          <p>{{ advice.suggestion }}</p>
        </div>
      </template>
      <el-empty v-else description="选择一条申请查看详情" />
    </aside>
  </div>
  <el-dialog v-model="visible" title="提交访问申请" width="500px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="字段ID"><el-input v-model="form.field_id" /></el-form-item>
      <el-form-item label="申请原因"><el-input v-model="form.reason" type="textarea" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="submit">提交</el-button></template>
  </el-dialog>
  <el-dialog v-model="approvalVisible" :title="approvalAction === 'approve' ? '审批通过' : '审批驳回'" width="520px">
    <el-form :model="approvalForm" label-width="100px">
      <el-form-item label="审批意见"><el-input v-model="approvalForm.comment" type="textarea" :rows="4" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="approvalVisible=false">取消</el-button><el-button type="primary" @click="submitApproval">确认</el-button></template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/http'
const props=defineProps({ mode:String })
const rows=ref([]), visible=ref(false), form=reactive({ field_id:4, reason:'业务排查需要查看原始样例值' })
const selected=ref(null), advice=ref(null), detail=ref(null), adviceLoading=ref(false), approvalVisible=ref(false), approvalAction=ref('approve')
const approvalForm=reactive({ comment:'' })
const pendingCount = computed(() => rows.value.filter(row => row.STATUS === 'PENDING').length)
const approvedCount = computed(() => rows.value.filter(row => row.STATUS === 'APPROVED').length)
const rejectedCount = computed(() => rows.value.filter(row => row.STATUS === 'REJECTED').length)
const highCount = computed(() => rows.value.filter(row => ['L4','L5'].includes(row.LEVEL_CODE)).length)
const policyNames = computed(() => (detail.value?.maskingPolicies || []).map(p=>p.POLICY_NAME).join('、') || '-')
const page=ref(1), pageSize=ref(20), total=ref(0)
async function load(){
  const res=await api.get(`/access-requests?page=${page.value}&pageSize=${pageSize.value}`)
  rows.value=res.rows; total.value=res.total
}
async function submit(){ const r=await api.post('/access-requests', form); if(r.duplicate){ ElMessage.warning(r.message); return } visible.value=false; ElMessage.success('申请已提交'); load() }
function approve(row){ openApproval(row,'approve') }
function reject(row){ openApproval(row,'reject') }
async function selectRow(row){
  selected.value=row; advice.value=null; detail.value=null
  if(row) detail.value = await api.get(`/access-requests/${row.ID}/detail`)
}
function openApproval(row, action){
  selected.value=row
  approvalAction.value=action
  approvalForm.comment = action === 'approve' ? '同意本次高敏字段访问，限定在有效期内用于申请所述业务场景。' : '驳回本次访问申请，当前理由不足或风险较高。'
  approvalVisible.value=true
}
async function submitApproval(){
  if(!approvalForm.comment.trim()){ ElMessage.warning('请填写审批意见'); return }
  await api.put(`/access-requests/${selected.value.ID}/${approvalAction.value}`, { comment: approvalForm.comment })
  approvalVisible.value=false
  ElMessage.success(approvalAction.value === 'approve' ? '已通过' : '已驳回')
  load()
}
async function review(row){
  selected.value=row
  adviceLoading.value=true
  try { advice.value = await api.post(`/agent/review-access-request/${row.ID}`, {}) }
  finally { adviceLoading.value=false }
}
function levelTag(level){ return level === 'L5' || level === 'L4' ? 'danger' : level === 'L3' ? 'warning' : 'success' }
function statusTag(status){ return status === 'APPROVED' ? 'success' : status === 'REJECTED' ? 'danger' : 'warning' }
function adviceTag(value){ return value === 'approve' ? 'success' : value === 'reject' ? 'danger' : 'warning' }
watch(()=>props.mode, load)
onMounted(load)
</script>
