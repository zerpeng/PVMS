<template>
  <header class="header">
    <div v-if="showSearch" class="search-container">
      <Search class="search-icon" />
      <input type="text" placeholder="搜索文件名、元数据或标签...">
    </div>

    <div class="header-actions" :class="{ 'push-right': showSearch }">
      <button class="icon-btn" type="button" @click="appStore.toggleTheme" :title="themeTitle">
        <Sun v-if="appStore.theme === 'dark'" />
        <Moon v-else />
      </button>
      <button v-if="!authInfo.visitor" class="icon-btn" type="button" title="上传媒体">
        <Plus />
      </button>

      <div class="user-menu">
        <button class="user-avatar" type="button" @click="toggleMenu" title="用户菜单">
          <User :size="21" />
          <span v-if="noticeCount > 0" class="notice-badge">{{ displayNoticeCount }}</span>
        </button>

        <div v-if="menuOpen" class="user-dropdown">
          <div class="profile-block">
            <div class="profile-avatar"><User :size="18" /></div>
            <div class="profile-copy">
              <strong>{{ profile.nickname || profile.username || '当前用户' }}</strong>
              <span>{{ profile.username || authInfo.username || '-' }}</span>
            </div>
          </div>

          <div class="profile-meta">
            <span>角色</span>
            <b>{{ profile.roleName || profile.roleKey || authInfo.roleKey }}</b>
          </div>

          <button v-if="canOpenMessages" class="menu-action" type="button" @click="openMessages">
            <Bell :size="16" />
            <span>消息</span>
            <em v-if="noticeCount > 0">{{ displayNoticeCount }}</em>
          </button>

          <button class="menu-action danger" type="button" @click="handleLogout">
            <LogOut :size="16" />
            <span>退出登录</span>
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, LogOut, Moon, Plus, Search, Sun, User } from 'lucide-vue-next'
import { useAppStore } from '@/stores/app'
import { authApi } from '@/api/auth'
import { applicationApi } from '@/api/application'
import { canShowMenu, getAuthInfo } from '@/utils/authz'

const appStore = useAppStore()
const route = useRoute()
const router = useRouter()
const menuOpen = ref(false)
const noticeCount = ref(0)
const profile = reactive({
  username: '',
  nickname: '',
  roleKey: '',
  roleName: ''
})

const themeTitle = computed(() => `切换至${appStore.theme === 'light' ? '深色' : '浅色'}模式`)
const showSearch = computed(() => route.name === 'library')
const authInfo = computed(() => getAuthInfo())
const canOpenMessages = computed(() => authInfo.value.superAdmin || canShowMenu('applications'))
const displayNoticeCount = computed(() => noticeCount.value > 99 ? '99+' : String(noticeCount.value))

const loadProfile = async () => {
  try {
    const data = await authApi.getProfile()
    Object.assign(profile, data || {})
  } catch (error) {
    profile.username = authInfo.value.username || ''
    profile.nickname = authInfo.value.nickname || ''
    profile.roleKey = authInfo.value.roleKey
    profile.roleName = authInfo.value.roleName || authInfo.value.roleKey
  }
}

const loadNoticeCount = async () => {
  if (!canOpenMessages.value) {
    noticeCount.value = 0
    return
  }
  try {
    const data = await applicationApi.fetchPendingCount()
    noticeCount.value = Number(data?.count || 0)
  } catch (error) {
    noticeCount.value = 0
  }
}

const toggleMenu = async () => {
  menuOpen.value = !menuOpen.value
  if (menuOpen.value) {
    await Promise.all([loadProfile(), loadNoticeCount()])
  }
}

const openMessages = () => {
  menuOpen.value = false
  router.push({ name: 'applications' })
}

const handleLogout = async () => {
  try {
    await authApi.logout()
  } catch (error) {
    console.error('Logout failed:', error)
  } finally {
    localStorage.removeItem('token')
    menuOpen.value = false
    router.replace({ name: 'login' })
  }
}

onMounted(() => {
  loadProfile()
  loadNoticeCount()
})
</script>

<style scoped>
.header {
  height: 90px;
  padding: 0 2.5rem;
  display: flex;
  align-items: center;
  gap: 1.25rem;
  backdrop-filter: blur(12px);
  z-index: 40;
  border-bottom: 1px solid var(--border);
}

[data-theme="light"] .header { background: rgba(248, 250, 252, 0.8); }
[data-theme="dark"] .header { background: rgba(2, 6, 23, 0.8); }
.search-container { flex: 0 1 520px; position: relative; min-width: 220px; }
.search-icon { position: absolute; left: 16px; top: 50%; transform: translateY(-50%); color: var(--text-muted); width: 18px; height: 18px; }
.search-container input { width: 100%; padding: 0.85rem 1rem 0.85rem 3rem; background: var(--surface); border: 1px solid var(--border); border-radius: 999px; outline: none; font-size: 0.9rem; color: var(--text); box-shadow: 0 2px 4px rgba(0,0,0,0.02); transition: all 0.2s; }
.search-container input:focus { border-color: var(--accent); box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.1); }
.header-actions { display: flex; gap: 1rem; align-items: center; margin-left: auto; }
.header-actions.push-right { margin-left: auto; }
.icon-btn { width: 44px; height: 44px; border-radius: 16px; background: var(--surface); border: 1px solid var(--border); color: var(--text); display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.2s; }
.icon-btn:hover { background: var(--surface-hover); transform: translateY(-2px); }
.user-menu { position: relative; }
.user-avatar { position: relative; width: 44px; height: 44px; border-radius: 16px; border: 2px solid var(--accent); color: var(--accent); background: var(--surface); cursor: pointer; display: flex; align-items: center; justify-content: center; }
.notice-badge { position: absolute; top: -7px; right: -8px; min-width: 20px; height: 20px; padding: 0 5px; border-radius: 999px; background: #dc2626; color: #fff; border: 2px solid var(--surface); font-size: 0.68rem; font-weight: 900; line-height: 16px; }
.user-dropdown { position: absolute; top: 56px; right: 0; width: 260px; padding: 12px; border-radius: 8px; background: var(--surface); border: 1px solid var(--border); box-shadow: var(--card-shadow-hover); z-index: 80; }
.profile-block { display: flex; align-items: center; gap: 12px; padding: 4px 4px 12px; border-bottom: 1px solid var(--border); }
.profile-avatar { width: 38px; height: 38px; border-radius: 8px; display: flex; align-items: center; justify-content: center; background: rgba(37,99,235,.12); color: var(--accent); }
.profile-copy { min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.profile-copy strong { color: var(--text); font-size: .95rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.profile-copy span { color: var(--text-muted); font-size: .78rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.profile-meta { display: flex; justify-content: space-between; gap: 12px; padding: 12px 4px; color: var(--text-muted); font-size: .78rem; }
.profile-meta b { max-width: 160px; color: var(--text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.menu-action { width: 100%; height: 40px; display: flex; align-items: center; gap: 9px; padding: 0 10px; border: none; border-radius: 8px; background: transparent; color: var(--text); font-weight: 800; cursor: pointer; }
.menu-action:hover { background: var(--surface-hover); }
.menu-action em { margin-left: auto; min-width: 22px; height: 22px; display: inline-flex; align-items: center; justify-content: center; border-radius: 999px; background: rgba(220,38,38,.1); color: #dc2626; font-style: normal; font-size: .72rem; }
.menu-action.danger { color: #dc2626; }
.menu-action.danger:hover { background: rgba(220, 38, 38, 0.08); }
@media (max-width: 900px) { .header { padding: 0 1.25rem; } .search-container { flex: 1 1 auto; min-width: 0; } .user-dropdown { right: -8px; } }
</style>
