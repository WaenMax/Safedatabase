<template>
  <h1 class="page-title">Agent 问答</h1>
  <el-card>
    <el-input v-model="question" type="textarea" :rows="3" placeholder="例如：哪些字段是高敏感数据？" />
    <div class="toolbar" style="margin-top:12px"><el-button type="primary" @click="ask">发送</el-button></div>
    <div v-if="answer" class="answer">{{ answer }}</div>
  </el-card>
  <el-card style="margin-top:16px">
    <template #header>可尝试的问题</template>
    <el-tag v-for="q in examples" :key="q" style="margin:4px; cursor:pointer" @click="question=q">{{ q }}</el-tag>
  </el-card>
</template>
<script setup>
import { ref } from 'vue'
import { api } from '../api/http'
const question=ref('哪些字段是高敏感数据？'), answer=ref('')
const examples=['哪些字段是高敏感数据？','哪些用户访问过 L4 数据？','最近有哪些风险告警？','哪些字段还没有分类分级？','哪些访问申请被拒绝？','当前系统的敏感字段数量是多少？','生成一份安全治理建议']
async function ask(){ const r = await api.post('/agent/chat',{ question: question.value }); answer.value = r.answer }
</script>
