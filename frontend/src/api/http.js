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
  get: (url) => http.get(url).then(r => r.data),
  post: (url, data) => http.post(url, data).then(r => r.data),
  put: (url, data) => http.put(url, data).then(r => r.data),
  del: (url) => http.delete(url).then(r => r.data)
}
