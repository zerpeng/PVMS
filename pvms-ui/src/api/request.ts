import axios from 'axios'

const request = axios.create({
  baseURL: '/api', // Vite 代理会将其转发至 8080
  timeout: 10000,
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 从本地缓存读取登录 Token，并统一放入 Authorization 请求头。
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // 后端通常返回 { code: 200, data: ..., message: ... }
    const res = response.data
    if (res.code !== 200 && res.code !== undefined) {
      console.error('API Error:', res.message || 'Error')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res.data || res
  },
  (error) => {
    // 收到 401 说明登录态失效，清理本地 Token 并跳转回登录页。
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    console.error('Network Error:', error)
    return Promise.reject(error)
  }
)

export default request
