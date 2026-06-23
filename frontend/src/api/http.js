import axios from 'axios'

export const http = axios.create({
  baseURL: 'http://localhost:8080/api'
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export const api = {
  get: (url) => http.get(url).then(r => r.data),
  post: (url, data) => http.post(url, data).then(r => r.data),
  put: (url, data) => http.put(url, data).then(r => r.data),
  del: (url) => http.delete(url).then(r => r.data)
}
