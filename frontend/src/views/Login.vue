<template>
  <div class="login">
    <el-form class="login-box" :model="form" label-position="top" @submit.prevent>
      <h2>数据分类分级保护系统</h2>
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
