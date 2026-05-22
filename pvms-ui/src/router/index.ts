import { createRouter, createWebHistory } from 'vue-router'
import { canShowMenu } from '@/utils/authz'

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
    {
      path: '/album-tags',
      name: 'album-tags',
      component: () => import('@/views/AlbumTags.vue'),
      meta: { layout: 'app', requiresAuth: true },
    },
    {
      path: '/favorites',
      name: 'favorites',
      component: () => import('@/views/Favorites.vue'),
      meta: { layout: 'app', requiresAuth: true },
    },
    {
      path: '/users',
      name: 'users',
      component: () => import('@/views/UserManagement.vue'),
      meta: { layout: 'app', requiresAuth: true },
    },
    {
      path: '/roles',
      name: 'roles',
      component: () => import('@/views/RoleManagement.vue'),
      meta: { layout: 'app', requiresAuth: true },
    },
    {
      path: '/applications',
      name: 'applications',
      component: () => import('@/views/ApplicationAudit.vue'),
      meta: { layout: 'app', requiresAuth: true },
    },
  ],
})

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

router.beforeEach((to, from, next) => {
  const isPublicPage = to.meta.public === true
  const tokenValid = hasValidToken()

  if (!isPublicPage && !tokenValid) {
    next({ name: 'login', query: { redirect: to.fullPath } })
  } else if (isPublicPage && tokenValid && to.name === 'login') {
    next({ name: 'library' })
  } else if (!isPublicPage && typeof to.name === 'string' && !canShowMenu(to.name)) {
    next({ name: 'library' })
  } else {
    next()
  }
})

export default router
