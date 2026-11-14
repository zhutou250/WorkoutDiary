import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('fitness_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) {
        return body.data
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      localStorage.removeItem('fitness_token')
      localStorage.removeItem('fitness_user')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
      ElMessage.error('登录已过期，请重新登录')
    } else {
      const message = error.response?.data?.message || error.message || '网络异常'
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default http
