<template>
  <div class="page-head">
    <div>
      <h1 class="page-title">脱敏策略管理</h1>
      <p>维护各类敏感数据的脱敏展示规则</p>
    </div>
  </div>
  <div class="toolbar">
    <el-button type="primary" @click="open({})">新增策略</el-button>
  </div>
  <el-table :data="rows" border>
    <el-table-column prop="ID" label="ID" width="70" />
    <el-table-column prop="POLICY_NAME" label="策略名称" />
    <el-table-column prop="POLICY_TYPE" label="类型" width="100" />
    <el-table-column prop="EXAMPLE_BEFORE" label="脱敏前示例" />
    <el-table-column prop="EXAMPLE_AFTER" label="脱敏后示例" />
    <el-table-column prop="DESCRIPTION" label="说明" />
    <el-table-column label="启用" width="80">
      <template #default="{ row }">
        <el-tag :type="row.ENABLED ? 'success' : 'info'">{{ row.ENABLED ? '启用' : '禁用' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="180">
      <template #default="{ row }">
        <el-button size="small" @click="open(row)">编辑</el-button>
        <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
      </template>
    </el-table-column>
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

  <el-dialog v-model="visible" :title="form.id ? '编辑脱敏策略' : '新增脱敏策略'" width="560px">
    <el-form :model="form" label-width="110px">
      <el-form-item label="策略名称">
        <el-input v-model="form.policy_name" placeholder="如：手机号脱敏" />
      </el-form-item>
      <el-form-item label="策略类型">
        <el-select v-model="form.policy_type" placeholder="选择类型" style="width: 100%">
          <el-option label="phone - 手机号" value="phone" />
          <el-option label="email - 邮箱" value="email" />
          <el-option label="id_card - 身份证" value="id_card" />
          <el-option label="bank_card - 银行卡" value="bank_card" />
          <el-option label="password - 密码密钥" value="password" />
          <el-option label="name - 姓名" value="name" />
          <el-option label="address - 地址" value="address" />
          <el-option label="custom - 自定义" value="custom" />
        </el-select>
      </el-form-item>
      <el-form-item label="脱敏前示例">
        <el-input v-model="form.example_before" placeholder="如：13812348888" />
      </el-form-item>
      <el-form-item label="脱敏后示例">
        <el-input v-model="form.example_after" placeholder="如：138****8888" />
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="form.description" type="textarea" :rows="2" placeholder="策略用途说明" />
      </el-form-item>
      <el-form-item label="启用">
        <el-switch v-model="form.enabled" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api/http'

const rows = ref([])
const visible = ref(false)
const form = reactive({ enabled: true })
const page = ref(1), pageSize = ref(20), total = ref(0)

async function load() {
  const res = await api.get(`/masking-policies?page=${page.value}&pageSize=${pageSize.value}`)
  rows.value = res.rows; total.value = res.total
}

function lower(row) {
  const obj = {}
  for (const k in row) obj[k.toLowerCase()] = row[k]
  return obj
}

function open(row) {
  Object.keys(form).forEach(k => delete form[k])
  if (row.ID) {
    Object.assign(form, lower(row))
    form.enabled = !!form.enabled
  } else {
    form.enabled = true
  }
  visible.value = true
}

async function save() {
  if (!form.policy_name || !form.policy_type) {
    ElMessage.warning('请填写策略名称和策略类型')
    return
  }
  if (form.id) {
    await api.put(`/masking-policies/${form.id}`, form)
  } else {
    await api.post('/masking-policies', form)
  }
  visible.value = false
  ElMessage.success('保存成功')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除脱敏策略「${row.POLICY_NAME}」？`)
  await api.del(`/masking-policies/${row.ID}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
