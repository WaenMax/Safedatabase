<template>
  <div class="login">
    <el-form class="login-box" :model="form" label-position="top" @submit.prevent>
      <h2>数据分类分级保护系统</h2>
      <div class="login-roles">
        <button v-for="role in roles" :key="role.username" type="button" @click="pick(role)">
          <strong>{{ role.label }}</strong>
          <span>{{ role.username }}</span>
        </button>
      </div>
      <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
      <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
      <el-button type="primary" style="width:100%" @click="login">登录</el-button>
      <p>演示账号：admin / security / user / approver，密码均为 123456</p>
    </el-form>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api/http'

const router = useRouter()
const form = reactive({ username: 'admin', password: '123456' })
const roles = [
  { label: '系统管理员', username: 'admin' },
  { label: '安全管理员', username: 'security' },
  { label: '普通用户', username: 'user' },
  { label: '审批人员', username: 'approver' }
]
function pick(role) {
  form.username = role.username
  form.password = '123456'
}
async function login() {
  try {
    const data = await api.post('/auth/login', form)
    localStorage.setItem('token', data.token)
    ElMessage.success('登录成功')
    await router.push('/')
    window.location.reload()
  } catch {
    ElMessage.error('用户名或密码错误')
  }
}
</script>
