<template>
  <header class="header">
    <div v-if="showSearch" class="search-container">
      <Search class="search-icon" />
      <input type="text" placeholder="搜索文件名、元数据或 AI 标签...">
    </div>

    <div class="header-actions" :class="{ 'push-right': showSearch }">
      <button class="icon-btn" @click="appStore.toggleTheme" :title="themeTitle">
        <Sun v-if="appStore.theme === 'dark'" />
        <Moon v-else />
      </button>
      <button class="icon-btn" title="上传媒体"><Plus /></button>
      <div class="user-menu">
        <button class="user-avatar" type="button" @click="menuOpen = !menuOpen" title="用户菜单">
          <div class="avatar-inner"></div>
        </button>
        <div v-if="menuOpen" class="user-dropdown">
          <button type="button" @click="handleLogout">
            <LogOut :size="16" />
            退出登录
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Sun, Moon, Plus, LogOut } from 'lucide-vue-next'
import { useAppStore } from '@/stores/app'
import { authApi } from '@/api/auth'

const appStore = useAppStore()
const route = useRoute()
const router = useRouter()
const menuOpen = ref(false)
const themeTitle = computed(() => `切换至${appStore.theme === 'light' ? '深色' : '浅色'}模式`)
const showSearch = computed(() => route.name === 'library')

// 退出登录先通知后端拉黑当前 Token，再清理前端缓存并跳转登录页。
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
</script>

<style scoped>
.header {
  height: 90px;
  padding: 0 2.5rem;
  display: flex;
  align-items: center;
  gap: 1.25rem;
  background: rgba(var(--bg-rgb), 0.8); /* We'll need to define bg-rgb if we want opacity */
  backdrop-filter: blur(12px);
  z-index: 40;
  border-bottom: 1px solid var(--border);
}

/* Fallback for background with blur */
[data-theme="light"] .header { background: rgba(248, 250, 252, 0.8); }
[data-theme="dark"] .header { background: rgba(2, 6, 23, 0.8); }

.search-container {
  flex: 0 1 520px;
  position: relative;
}

.search-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  width: 18px;
  height: 18px;
}

.search-container input {
  width: 100%;
  padding: 0.85rem 1rem 0.85rem 3rem;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 100px;
  outline: none;
  font-size: 0.9rem;
  color: var(--text);
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
  transition: all 0.2s;
}

.search-container input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.1);
}

.header-actions {
  display: flex;
  gap: 1rem;
  align-items: center;
  margin-left: auto;
}

.header-actions.push-right {
  margin-left: auto;
}

.icon-btn {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  background: var(--surface);
  border: 1px solid var(--border);
  color: var(--text);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: var(--surface-hover);
  transform: translateY(-2px);
}

.user-menu {
  position: relative;
}

.user-avatar {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  overflow: hidden;
  border: 2px solid var(--accent);
  padding: 2px;
  background: transparent;
  cursor: pointer;
}

.avatar-inner {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  background: #e2e8f0;
  background-image: url('https://api.dicebear.com/7.x/avataaars/svg?seed=Felix');
  background-size: cover;
}

.user-dropdown {
  position: absolute;
  top: 54px;
  right: 0;
  min-width: 138px;
  padding: 8px;
  border-radius: 14px;
  background: var(--surface);
  border: 1px solid var(--border);
  box-shadow: var(--card-shadow-hover);
  z-index: 80;
}

.user-dropdown button {
  width: 100%;
  height: 38px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #dc2626;
  font-weight: 700;
  cursor: pointer;
}

.user-dropdown button:hover {
  background: rgba(220, 38, 38, 0.08);
}

@media (max-width: 900px) {
  .header {
    padding: 0 1.25rem;
  }

  .search-container {
    flex: 1 1 auto;
    min-width: 0;
  }
}
</style>
