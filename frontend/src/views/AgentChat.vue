<template>
  <h1 class="page-title">Agent 问答</h1>
  <el-card>
    <el-input v-model="question" type="textarea" :rows="3" placeholder="例如：哪些字段是高敏感数据？" />
    <div class="toolbar" style="margin-top:12px"><el-button type="primary" @click="ask">发送</el-button></div>
    <div v-if="answer" class="answer markdown-body" v-html="renderMarkdown(answer)"></div>
    <div v-if="actions.length" class="agent-action-row">
      <el-button v-for="action in actions" :key="action.path" @click="$router.push(action.path)">{{ action.label }}</el-button>
    </div>
    <div v-if="references.length" class="reference-panel">
      <strong>回答引用的数据</strong>
      <div v-for="item in references" :key="item.title + item.source" class="reference-item">
        <span>{{ item.title }}</span>
        <small>{{ item.source }}</small>
        <b>{{ item.count }}</b>
        <div v-if="item.rows?.length" class="reference-rows">
          <el-tag v-for="row in item.rows.slice(0, 5)" :key="JSON.stringify(row)" size="small">{{ row.FIELD_NAME || row.DESCRIPTION || row.RISK_TYPE || row.USERNAME || row.REASON || row.ID }}</el-tag>
        </div>
      </div>
    </div>
  </el-card>
  <el-card style="margin-top:16px">
    <template #header>可尝试的问题</template>
    <el-tag v-for="q in examples" :key="q" style="margin:4px; cursor:pointer" @click="question=q">{{ q }}</el-tag>
  </el-card>
</template>
<script setup>
import { ref } from 'vue'
import { api } from '../api/http'
import { renderMarkdown } from '../utils/markdown'
const question=ref('哪些字段是高敏感数据？'), answer=ref(''), references=ref([]), actions=ref([])
const examples=['哪些字段是高敏感数据？','哪些用户访问过 L4 数据？','最近有哪些风险告警？','哪些字段还没有分类分级？','哪些访问申请被拒绝？','当前系统的敏感字段数量是多少？','生成一份安全治理建议']
async function ask(){ const r = await api.post('/agent/chat',{ question: question.value }); answer.value = r.answer; references.value = r.references || []; actions.value = r.actions || [] }
</script>
