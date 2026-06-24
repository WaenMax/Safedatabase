import axios from 'axios'
import { ElMessage } from 'element-plus'

export const http = axios.create({
  baseURL: 'http://localhost:8080/api'
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  response => response,
  error => {
    const status = error?.response?.status
    const message = error?.response?.data?.message
    if (status === 403) ElMessage.error(message || '当前角色无权限')
    else if (status >= 500) ElMessage.error('后端服务异常，请查看日志')
    else if (message) ElMessage.error(message)
    return Promise.reject(error)
  }
)

export const api = {
  get: (url) => http.get(url).then(r => normalizeKeys(r.data)),
  post: (url, data) => http.post(url, data).then(r => normalizeKeys(r.data)),
  put: (url, data) => http.put(url, data).then(r => normalizeKeys(r.data)),
  del: (url) => http.delete(url).then(r => normalizeKeys(r.data))
}

function normalizeKeys(value) {
  if (Array.isArray(value)) return value.map(normalizeKeys)
  if (!value || typeof value !== 'object') return value

  const normalized = {}
  for (const [key, raw] of Object.entries(value)) {
    const child = normalizeKeys(raw)
    normalized[key] = child
    const upperKey = key.toUpperCase()
    if (!(upperKey in normalized)) normalized[upperKey] = child
  }
  return normalized
}
