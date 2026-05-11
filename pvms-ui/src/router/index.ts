import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/Auth.vue'),
      meta: { layout: 'auth', public: true },
    },
    {
      path: '/',
      redirect: '/media',
    },
    {
      path: '/media',
      name: 'library',
      component: () => import('@/views/Library.vue'),
      meta: { layout: 'app', requiresAuth: true },
    },
    {
      path: '/projects',
      name: 'projects',
      component: () => import('@/views/Project.vue'),
      meta: { layout: 'app', requiresAuth: true },
    },
  ],
})

// 解析 JWT 载荷，用于在前端路由层提前判断 Token 是否过期。
const parseJwtPayload = (token: string) => {
  const payload = token.split('.')[1]
  if (!payload) return null

  const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
  const json = decodeURIComponent(
    atob(normalized)
      .split('')
      .map(char => `%${(`00${char.charCodeAt(0).toString(16)}`).slice(-2)}`)
      .join('')
  )
  return JSON.parse(json)
}

// 校验本地 Token 是否存在、格式正确且未过期；无效时主动清理缓存。
const hasValidToken = () => {
  const token = localStorage.getItem('token')
  if (!token || token === 'undefined' || token === 'null') {
    return false
  }

  try {
    const payload = parseJwtPayload(token)
    if (!payload?.exp || payload.exp * 1000 <= Date.now()) {
      localStorage.removeItem('token')
      return false
    }
    return true
  } catch (error) {
    localStorage.removeItem('token')
    return false
  }
}

// 全局路由守卫：除 public 页面外默认都需要登录，避免新增页面漏配 requiresAuth。
router.beforeEach((to, from, next) => {
  const isPublicPage = to.meta.public === true
  const tokenValid = hasValidToken()

  if (!isPublicPage && !tokenValid) {
    next({ name: 'login', query: { redirect: to.fullPath } })
  } else if (isPublicPage && tokenValid && to.name === 'login') {
    next({ name: 'library' })
  } else {
    next()
  }
})

export default router
