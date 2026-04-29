<template>
  <header class="header">
    <div class="header-left">
      <div class="breadcrumb">
        <span>媒体库</span> 
        <ChevronRight class="breadcrumb-sep" /> 
        <span>影棚 A</span>
      </div>
      <h2 class="page-title">工作空间</h2>
    </div>
    
    <div class="search-container">
      <Search class="search-icon" />
      <input type="text" placeholder="搜索文件名、元数据或 AI 标签...">
    </div>

    <div class="header-actions">
      <button class="icon-btn" @click="appStore.toggleTheme" :title="themeTitle">
        <Sun v-if="appStore.theme === 'dark'" />
        <Moon v-else />
      </button>
      <button class="icon-btn" title="上传媒体"><Plus /></button>
      <div class="user-avatar">
        <div class="avatar-inner"></div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ChevronRight, Search, Sun, Moon, Plus } from 'lucide-vue-next'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const themeTitle = computed(() => `切换至${appStore.theme === 'light' ? '深色' : '浅色'}模式`)
</script>

<style scoped>
.header {
  height: 90px;
  padding: 0 2.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(var(--bg-rgb), 0.8); /* We'll need to define bg-rgb if we want opacity */
  backdrop-filter: blur(12px);
  z-index: 40;
  border-bottom: 1px solid var(--border);
}

/* Fallback for background with blur */
[data-theme="light"] .header { background: rgba(248, 250, 252, 0.8); }
[data-theme="dark"] .header { background: rgba(2, 6, 23, 0.8); }

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 0.85rem;
  margin-bottom: 4px;
}

.breadcrumb-sep {
  width: 12px;
  height: 12px;
}

.page-title {
  font-size: 1.5rem;
  letter-spacing: -0.5px;
  color: var(--primary);
}

.search-container {
  flex: 0 1 450px;
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

.user-avatar {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  overflow: hidden;
  border: 2px solid var(--accent);
  padding: 2px;
}

.avatar-inner {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  background: #e2e8f0;
  background-image: url('https://api.dicebear.com/7.x/avataaars/svg?seed=Felix');
  background-size: cover;
}
</style>
