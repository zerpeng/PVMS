<template>
  <nav class="sidebar">
    <div class="logo">
      <Aperture class="logo-icon" />
      <span>PVMS STUDIO</span>
    </div>

    <ul class="nav-list">
      <li
        v-for="item in menuItems"
        :key="item.label"
        :class="['nav-item', { active: item.routeName && route.name === item.routeName, disabled: !item.routeName }]"
        @click="goToMenu(item.routeName)"
      >
        <component :is="item.icon" class="nav-icon" />
        {{ item.label }}
      </li>

      <div class="divider"></div>

      <li class="nav-item disabled"><Cpu class="nav-icon" />AI 处理器</li>
      <li class="nav-item disabled"><Settings class="nav-icon" />系统设置</li>
    </ul>

    <div class="storage-card">
      <p class="storage-label">已用存储空间</p>
      <div class="progress-bar">
        <div class="progress-fill" style="width: 65%"></div>
      </div>
      <p class="storage-stats">842 GB / 1.2 TB</p>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import {
  Aperture,
  LayoutGrid,
  FolderOpen,
  Tag,
  Heart,
  Clock,
  Users,
  Cpu,
  Settings,
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

const menuItems = [
  { label: '媒体库', icon: LayoutGrid, routeName: 'library' },
  { label: '项目工程', icon: FolderOpen, routeName: 'projects' },
  { label: '相册标签', icon: Tag, routeName: 'album-tags' },
  { label: '我的收藏', icon: Heart, routeName: 'favorites' },
  { label: '用户管理', icon: Users, routeName: 'users' },
  { label: '最近访问', icon: Clock, routeName: '' },
]

const goToMenu = (routeName: string) => {
  if (!routeName) return
  router.push({ name: routeName })
}
</script>

<style scoped>
.sidebar {
  width: 280px;
  background: var(--surface);
  padding: 2rem 1.25rem;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border);
  z-index: 50;
  height: 100vh;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 800;
  font-size: 1.4rem;
  margin-bottom: 2.5rem;
  padding-left: 0.75rem;
  color: var(--primary);
  letter-spacing: -0.5px;
}

.logo-icon {
  color: var(--accent);
  width: 28px;
  height: 28px;
}

.nav-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0.85rem 1rem;
  border-radius: 18px;
  cursor: pointer;
  color: var(--text-muted);
  font-weight: 500;
  font-size: 0.95rem;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.nav-icon {
  width: 20px;
  height: 20px;
}

.nav-item:hover {
  background: var(--surface-hover);
  color: var(--text);
  transform: translateX(4px);
}

.nav-item.active {
  background: var(--accent);
  color: white;
  box-shadow: 0 10px 15px -3px rgba(37, 99, 235, 0.3);
}

.nav-item.disabled {
  cursor: default;
  opacity: 0.58;
}

.nav-item.disabled:hover {
  background: transparent;
  color: var(--text-muted);
  transform: none;
}

.divider {
  height: 1px;
  background: var(--border);
  margin: 1.5rem 0.5rem;
}

.storage-card {
  margin-top: auto;
  padding: 1.5rem;
  background: var(--surface-hover);
  border-radius: 20px;
}

.storage-label {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-bottom: 0.75rem;
  font-weight: 600;
}

.progress-bar {
  height: 6px;
  background: var(--border);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--accent);
}

.storage-stats {
  font-size: 0.75rem;
  margin-top: 0.5rem;
  font-weight: 600;
}
</style>
