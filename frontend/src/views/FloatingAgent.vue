<template>
  <div v-if="open" class="floating-agent">
    <div class="agent-head">
      <div>
        <strong>AI 数据安全助手</strong>
        <span>{{ providerLabel }}</span>
      </div>
      <el-button text @click="open=false">×</el-button>
    </div>
    <div class="agent-controls">
      <el-select v-model="provider" size="small" style="width: 140px">
        <el-option label="本地规则" value="local" />
        <el-option label="DeepSeek 官方" value="deepseek" />
        <el-option label="硅基流动" value="siliconflow" />
      </el-select>
      <el-input v-model="model" size="small" placeholder="如 deepseek-v4-flash / deepseek-v4-pro" />
    </div>
    <div class="agent-messages">
      <div v-for="(m, i) in messages" :key="i" :class="['agent-msg', m.role]">
        <div>
          <div class="markdown-body" v-html="renderMarkdown(m.content)"></div>
          <div v-if="m.actions?.length" class="mini-actions">
            <button v-for="action in m.actions" :key="action.path" @click="$router.push(action.path)">{{ action.label }}</button>
          </div>
          <div v-if="m.references?.length" class="mini-references">
            <span v-for="item in m.references" :key="item.title + item.source">{{ item.title }} · {{ item.count }}</span>
          </div>
        </div>
      </div>
    </div>
    <div class="agent-quick">
      <el-tag v-for="q in quickQuestions" :key="q" size="small" @click="question=q">{{ q }}</el-tag>
    </div>
    <div class="agent-input">
      <el-input v-model="question" type="textarea" :rows="2" placeholder="问我数据分类、审批、审计风险或整改建议" @keydown.ctrl.enter="send" />
      <el-button type="primary" :loading="loading" @click="send">发送</el-button>
    </div>
  </div>
  <button v-else class="agent-fab" @click="open=true">AI</button>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/http'
import { renderMarkdown } from '../utils/markdown'

const open = ref(false)
const loading = ref(false)
const provider = ref(localStorage.getItem('agent_provider') || 'local')
const model = ref(localStorage.getItem('agent_model') || '')
const question = ref('哪些字段是高敏感数据？')
const messages = ref([
  { role: 'assistant', content: '你好，我是数据安全治理 Agent。可以帮你分析字段、审批申请、审计风险和整改建议。' }
])
const quickQuestions = ['系统概览', '有哪些高敏感字段？', '最近有哪些风险告警？', '有几个待审批申请？', '哪些字段还没分类？', '生成一份安全治理建议']
const providerLabel = computed(() => ({ local: '本地规则推理', deepseek: 'DeepSeek 官方 API', siliconflow: '硅基流动 API' }[provider.value]))

watch(provider, v => localStorage.setItem('agent_provider', v))
watch(model, v => localStorage.setItem('agent_model', v))

async function send() {
  const q = question.value.trim()
  if (!q) return
  messages.value.push({ role: 'user', content: q })
  loading.value = true
  try {
    const res = await api.post('/agent/chat', { question: q, provider: provider.value, model: model.value })
    messages.value.push({ role: 'assistant', content: res.answer, references: res.references || [], actions: res.actions || [] })
    question.value = ''
  } catch (e) {
    const msg = e?.response?.data?.message || 'AI 助手请求失败，请检查后端日志或 API Key 配置。'
    ElMessage.error(msg)
    messages.value.push({ role: 'assistant', content: msg })
  } finally {
    loading.value = false
  }
}
</script>

