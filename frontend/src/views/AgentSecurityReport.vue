<template>
  <h1 class="page-title">安全报告</h1>
  <div class="toolbar"><el-button type="primary" @click="generate">生成报告</el-button></div>
  <div class="markdown markdown-body" v-html="renderMarkdown(markdown || '点击“生成报告”获取 Markdown 格式的数据安全治理报告。')"></div>
</template>
<script setup>
import { ref } from 'vue'
import { api } from '../api/http'
import { renderMarkdown } from '../utils/markdown'
const markdown=ref('')
async function generate(){ const r = await api.get('/agent/security-report'); markdown.value = r.markdown }
</script>
