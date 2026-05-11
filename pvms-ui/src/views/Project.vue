<template>
  <div class="project-view">
    <header class="project-header">
      <div>
        <p class="eyebrow">监控目录</p>
        <h1 class="page-title">项目工程</h1>
      </div>

      <div class="header-actions">
        <label class="sync-toggle" :class="{ active: syncScanEnabled }">
          <input type="checkbox" :checked="syncScanEnabled" :disabled="preferenceLoading" @change="toggleSyncScan">
          <span>同步扫描</span>
        </label>
        <label class="select-all">
          <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll">
          <span>全选</span>
        </label>
        <button class="scan-btn" type="button" :disabled="selectedIds.length === 0 || loading" @click="scanSelectedDirectories">
          <RefreshCw :size="17" />
          扫描选中
        </button>
        <button class="remove-btn" type="button" :disabled="selectedIds.length === 0 || loading" @click="removeSelectedDirectories">
          <Trash2 :size="17" />
          移除选中
        </button>
      </div>
    </header>

    <section class="directory-grid">
      <button class="add-card" type="button" @click="openAddDialog">
        <div class="add-icon">
          <Plus :size="44" />
        </div>
        <span>新增监控目录</span>
      </button>

      <article
        v-for="directory in directories"
        :key="directory.id"
        :class="['directory-card', { selected: selectedIds.includes(directory.id) }]"
        @click="toggleDirectory(directory.id)"
      >
        <input
          class="directory-check"
          type="checkbox"
          :checked="selectedIds.includes(directory.id)"
          @click.stop
          @change="toggleDirectory(directory.id)"
        >
        <Folder class="folder-icon" :size="86" />
        <h3>{{ getDirectoryName(directory.dirPath) }}</h3>
        <p>{{ directory.dirPath }}</p>
        <div class="directory-meta">
          <span>{{ directory.isRecursive ? '递归扫描' : '当前层级' }}</span>
          <span>{{ directory.scanStatus === 1 ? '扫描中' : formatTime(directory.lastScanTime) }}</span>
        </div>
      </article>
    </section>

    <transition name="dialog-fade">
      <div v-if="addDialogOpen" class="dialog-overlay">
        <div class="dialog-card">
          <h2>新增监控目录</h2>
          <p>请输入本机或 NAS 挂载后的目录路径，后端会校验该路径是否存在。</p>
          <input v-model="newDirectoryPath" class="path-input" type="text" placeholder="例如：D:/Photos/2026">
          <label class="recursive-option">
            <input v-model="newDirectoryRecursive" type="checkbox">
            <span>包含子目录</span>
          </label>
          <div class="dialog-actions">
            <button class="ghost-btn" type="button" @click="addDialogOpen = false">取消</button>
            <button class="primary-btn" type="button" :disabled="!newDirectoryPath.trim() || loading" @click="addDirectory">添加</button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="dialog-fade">
      <div v-if="messageDialog.visible" class="dialog-overlay message-overlay" @click.self="closeMessageDialog">
        <div class="dialog-card message-dialog" :class="`message-${messageDialog.type}`">
          <div class="message-header">
            <h2>{{ messageDialog.title }}</h2>
          </div>
          <p class="message-text">{{ messageDialog.message }}</p>
          <div class="dialog-actions">
            <button class="primary-btn" type="button" @click="closeMessageDialog">知道了</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Folder, Plus, RefreshCw, Trash2 } from 'lucide-vue-next'
import { mediaApi } from '@/api/media'

interface MonitorDirectory {
  id: number
  dirPath: string
  isRecursive?: number
  scanStatus?: number
  lastScanTime?: string
  createTime?: string
}

type MessageDialogType = 'info' | 'success' | 'error'

const directories = ref<MonitorDirectory[]>([])
const selectedIds = ref<number[]>([])
const loading = ref(false)
const preferenceLoading = ref(false)
const addDialogOpen = ref(false)
const newDirectoryPath = ref('')
const newDirectoryRecursive = ref(true)
const syncScanEnabled = ref(false)

const messageDialog = reactive({
  visible: false,
  title: '',
  message: '',
  type: 'info' as MessageDialogType
})

const isAllSelected = computed(() => directories.value.length > 0 && selectedIds.value.length === directories.value.length)

const extractErrorMessage = (error: any, fallback: string) => {
  const responseData = error?.response?.data
  if (typeof responseData?.message === 'string' && responseData.message.trim()) {
    return responseData.message
  }
  if (typeof responseData === 'string' && responseData.trim()) {
    return responseData
  }
  if (typeof error?.message === 'string' && error.message.trim() && error.message !== '[object Object]') {
    return error.message
  }
  return fallback
}

const showMessageDialog = (title: string, message: string, type: MessageDialogType = 'info') => {
  messageDialog.title = title
  messageDialog.message = message
  messageDialog.type = type
  messageDialog.visible = true
}

const closeMessageDialog = () => {
  messageDialog.visible = false
}

const loadDirectories = async () => {
  loading.value = true
  try {
    directories.value = await mediaApi.fetchDirectories()
    const existingIds = new Set(directories.value.map((directory) => directory.id))
    selectedIds.value = selectedIds.value.filter((id) => existingIds.has(id))
  } catch (error) {
    console.error('Failed to load directories:', error)
  } finally {
    loading.value = false
  }
}

const loadPreference = async () => {
  preferenceLoading.value = true
  try {
    const preference = await mediaApi.fetchPreference()
    syncScanEnabled.value = Boolean(preference?.syncScanEnabled)
  } catch (error) {
    console.error('Failed to load media preference:', error)
  } finally {
    preferenceLoading.value = false
  }
}

const getDirectoryName = (path: string) => {
  const normalized = path.replace(/\\/g, '/')
  return normalized.split('/').filter(Boolean).pop() || path
}

const formatTime = (time?: string) => {
  if (!time) return '待扫描'
  return time.replace('T', ' ').slice(0, 16)
}

const openAddDialog = () => {
  newDirectoryPath.value = ''
  newDirectoryRecursive.value = true
  addDialogOpen.value = true
}

const addDirectory = async () => {
  loading.value = true
  try {
    const directory = await mediaApi.addDirectory({
      dirPath: newDirectoryPath.value.trim(),
      isRecursive: newDirectoryRecursive.value ? 1 : 0,
      scanStatus: 0
    })

    if (syncScanEnabled.value && directory?.id) {
      await mediaApi.scanDirectories([directory.id])
    }

    addDialogOpen.value = false
    await loadDirectories()
    showMessageDialog('添加成功', '监控目录已添加。', 'success')
  } catch (error: any) {
    showMessageDialog('添加失败', extractErrorMessage(error, '新增监控目录失败'), 'error')
  } finally {
    loading.value = false
  }
}

const toggleSyncScan = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const nextValue = target.checked
  const previousValue = syncScanEnabled.value

  syncScanEnabled.value = nextValue
  preferenceLoading.value = true
  try {
    const preference = await mediaApi.updateSyncScanPreference(nextValue)
    syncScanEnabled.value = Boolean(preference?.syncScanEnabled)
  } catch (error: any) {
    syncScanEnabled.value = previousValue
    showMessageDialog('保存失败', extractErrorMessage(error, '同步扫描设置保存失败'), 'error')
  } finally {
    preferenceLoading.value = false
  }
}

const toggleDirectory = (id: number) => {
  selectedIds.value = selectedIds.value.includes(id)
    ? selectedIds.value.filter((selectedId) => selectedId !== id)
    : [...selectedIds.value, id]
}

const toggleSelectAll = () => {
  selectedIds.value = isAllSelected.value ? [] : directories.value.map((directory) => directory.id)
}

const scanSelectedDirectories = async () => {
  if (selectedIds.value.length === 0) return

  loading.value = true
  try {
    const message = await mediaApi.scanDirectories(selectedIds.value)
    await loadDirectories()
    showMessageDialog('扫描已启动', typeof message === 'string' ? message : `已启动 ${selectedIds.value.length} 个扫描任务`, 'success')
  } catch (error: any) {
    showMessageDialog('扫描失败', extractErrorMessage(error, '扫描任务启动失败'), 'error')
  } finally {
    loading.value = false
  }
}

const removeSelectedDirectories = async () => {
  if (selectedIds.value.length === 0) return
  if (!confirm(`确认移除 ${selectedIds.value.length} 个监控目录吗？`)) return

  loading.value = true
  try {
    await Promise.all(selectedIds.value.map((id) => mediaApi.removeDirectory(id)))
    selectedIds.value = []
    await loadDirectories()
    showMessageDialog('移除成功', '选中的监控目录已移除。', 'success')
  } catch (error: any) {
    showMessageDialog('移除失败', extractErrorMessage(error, '移除监控目录失败'), 'error')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDirectories()
  loadPreference()
})
</script>

<style scoped>
.project-view {
  width: 100%;
}

.project-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 28px;
}

.eyebrow {
  color: var(--text-muted);
  font-size: 0.85rem;
  font-weight: 800;
}

.page-title {
  margin-top: 4px;
  color: var(--text);
  font-size: 1.8rem;
  font-weight: 900;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sync-toggle,
.select-all,
.scan-btn,
.remove-btn {
  height: 44px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 14px;
  font-weight: 700;
}

.sync-toggle,
.select-all {
  padding: 0 14px;
  color: var(--text);
  background: var(--surface);
  border: 1px solid var(--border);
}

.sync-toggle {
  cursor: pointer;
}

.sync-toggle.active {
  color: var(--accent);
  border-color: rgba(37, 99, 235, 0.42);
  background: rgba(37, 99, 235, 0.08);
}

.scan-btn,
.remove-btn {
  padding: 0 16px;
  border: none;
  color: #fff;
  cursor: pointer;
}

.scan-btn {
  background: var(--accent);
}

.remove-btn {
  background: #dc2626;
}

.scan-btn:disabled,
.remove-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.directory-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 24px;
}

.add-card,
.directory-card {
  min-height: 240px;
  border-radius: 24px;
  border: 1px solid var(--border);
  background: var(--surface);
  box-shadow: var(--card-shadow);
}

.add-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  color: var(--accent);
  cursor: pointer;
  border-style: dashed;
}

.add-icon {
  width: 112px;
  height: 112px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 30px;
  background: rgba(37, 99, 235, 0.1);
}

.add-card span {
  color: var(--text);
  font-weight: 900;
}

.directory-card {
  position: relative;
  padding: 24px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.directory-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--card-shadow-hover);
}

.directory-card.selected {
  border-color: var(--accent);
  box-shadow: 0 18px 40px rgba(37, 99, 235, 0.18);
}

.directory-check {
  position: absolute;
  top: 18px;
  right: 18px;
  width: 18px;
  height: 18px;
}

.folder-icon {
  margin-top: 6px;
  color: #f59e0b;
  fill: rgba(245, 158, 11, 0.18);
}

.directory-card h3 {
  margin-top: 18px;
  color: var(--text);
  font-size: 1.05rem;
  word-break: break-word;
}

.directory-card p {
  margin-top: 8px;
  color: var(--text-muted);
  font-size: 0.82rem;
  line-height: 1.5;
  word-break: break-all;
}

.directory-meta {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.directory-meta span {
  padding: 5px 8px;
  border-radius: 999px;
  color: var(--text-muted);
  background: var(--surface-hover);
  font-size: 0.72rem;
  font-weight: 700;
}

.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 220;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.22);
  backdrop-filter: blur(4px);
}

.dialog-card {
  width: min(460px, 100%);
  padding: 28px;
  border-radius: 24px;
  background: var(--surface);
  border: 1px solid var(--border);
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.24);
}

.dialog-card h2 {
  color: var(--text);
  font-size: 1.25rem;
}

.dialog-card p {
  margin-top: 8px;
  color: var(--text-muted);
  line-height: 1.6;
}

.path-input {
  width: 100%;
  margin-top: 20px;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--surface-hover);
  color: var(--text);
  outline: none;
}

.recursive-option {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text);
  font-weight: 700;
}

.dialog-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.ghost-btn,
.primary-btn {
  height: 42px;
  padding: 0 18px;
  border-radius: 14px;
  font-weight: 800;
  cursor: pointer;
}

.ghost-btn {
  border: 1px solid var(--border);
  color: var(--text);
  background: var(--surface);
}

.primary-btn {
  border: none;
  color: #fff;
  background: var(--accent);
}

.primary-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.message-overlay {
  z-index: 230;
}

.message-dialog {
  width: min(420px, 100%);
}

.message-header h2 {
  margin: 0;
}

.message-text {
  margin-top: 14px;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-success {
  border-color: rgba(22, 163, 74, 0.22);
}

.message-error {
  border-color: rgba(220, 38, 38, 0.22);
}

.message-info {
  border-color: rgba(37, 99, 235, 0.22);
}

.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
  transform: scale(0.98);
}

@media (max-width: 720px) {
  .project-header {
    align-items: stretch;
    flex-direction: column;
  }

  .header-actions {
    flex-wrap: wrap;
    justify-content: space-between;
  }
}
</style>
