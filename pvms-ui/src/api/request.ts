import axios from 'axios'

const request = axios.create({
  baseURL: '/api', // Vite 代理会将其转发至 8080
  timeout: 10000,
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 预留：此处可以添加 Token 等认证信息
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
    console.error('Network Error:', error)
    return Promise.reject(error)
  }
)

export default request
