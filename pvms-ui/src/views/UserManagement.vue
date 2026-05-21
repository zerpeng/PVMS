<template>
  <div class="user-view">
    <header class="user-header">
      <div>
        <p class="eyebrow">SYSTEM ACCESS</p>
        <h1 class="page-title">用户管理</h1>
      </div>
      <div class="header-actions">
        <button class="action-btn ghost" type="button" :disabled="loading" @click="triggerImport">
          <Upload :size="17" /> 导入
        </button>
        <button class="action-btn ghost" type="button" :disabled="loading" @click="exportUsers">
          <Download :size="17" /> 导出
        </button>
        <button class="action-btn primary" type="button" @click="openCreateDialog">
          <UserPlus :size="17" /> 新增用户
        </button>
        <input ref="fileInputRef" class="file-input" type="file" accept=".csv,text/csv" @change="handleImport">
      </div>
    </header>

    <section class="filter-panel">
      <label class="field">
        <span>用户名</span>
        <input v-model.trim="filters.username" type="text" placeholder="输入用户名">
      </label>
      <label class="field">
        <span>昵称</span>
        <input v-model.trim="filters.nickname" type="text" placeholder="输入昵称">
      </label>
      <label class="field">
        <span>角色</span>
        <select v-model="filters.roleKey">
          <option value="">全部角色</option>
          <option v-for="role in roleOptions" :key="role.value" :value="role.value">{{ role.label }}</option>
        </select>
      </label>
      <label class="field">
        <span>状态</span>
        <select v-model="filters.status">
          <option value="">全部状态</option>
          <option :value="1">启用</option>
          <option :value="0">禁用</option>
        </select>
      </label>
      <div class="filter-actions">
        <button class="icon-text-btn" type="button" :disabled="loading" @click="loadUsers(1)">
          <Search :size="17" /> 查询
        </button>
        <button class="icon-text-btn muted" type="button" :disabled="loading" @click="resetFilters">
          <RotateCcw :size="17" /> 重置
        </button>
      </div>
    </section>

    <section class="table-shell">
      <table class="user-table">
        <thead>
          <tr>
            <th>用户名</th>
            <th>昵称</th>
            <th>角色</th>
            <th>状态</th>
            <th>创建时间</th>
            <th class="actions-col">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="6" class="empty-cell">加载中...</td>
          </tr>
          <tr v-else-if="users.length === 0">
            <td colspan="6" class="empty-cell">暂无用户</td>
          </tr>
          <tr v-for="user in users" v-else :key="user.id">
            <td>
              <div class="user-name">
                <span class="avatar">{{ getInitial(user) }}</span>
                <span>{{ user.username }}</span>
              </div>
            </td>
            <td>{{ user.nickname || '-' }}</td>
            <td><span class="role-chip">{{ getRoleLabel(user.roleKey) }}</span></td>
            <td>
              <span :class="['status-pill', user.status === 1 ? 'enabled' : 'disabled']">
                {{ user.status === 1 ? '启用' : '禁用' }}
              </span>
            </td>
            <td>{{ formatTime(user.createTime) }}</td>
            <td>
              <div class="row-actions">
                <button class="icon-btn" type="button" title="编辑" @click="openEditDialog(user)">
                  <Pencil :size="16" />
                </button>
                <button class="icon-btn danger" type="button" title="删除" @click="removeUser(user)">
                  <Trash2 :size="16" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <footer class="pagination-bar">
      <span>共 {{ total }} 条</span>
      <div class="pager">
        <button type="button" :disabled="pagination.current <= 1 || loading" @click="loadUsers(pagination.current - 1)">
          <ChevronLeft :size="16" />
        </button>
        <span>{{ pagination.current }} / {{ pageCount }}</span>
        <button type="button" :disabled="pagination.current >= pageCount || loading" @click="loadUsers(pagination.current + 1)">
          <ChevronRight :size="16" />
        </button>
      </div>
    </footer>

    <transition name="dialog-fade">
      <div v-if="dialogOpen" class="dialog-overlay" @click.self="closeDialog">
        <form class="dialog-panel" @submit.prevent="submitUser">
          <header class="dialog-header">
            <h2>{{ editingUser ? '修改用户' : '新增用户' }}</h2>
            <button class="plain-icon" type="button" title="关闭" @click="closeDialog">
              <X :size="18" />
            </button>
          </header>

          <div class="form-grid">
            <label class="field">
              <span>用户名</span>
              <input v-model.trim="form.username" type="text" required>
            </label>
            <label class="field">
              <span>昵称</span>
              <input v-model.trim="form.nickname" type="text">
            </label>
            <label class="field">
              <span>密码</span>
              <input v-model="form.password" type="password" :required="!editingUser" :placeholder="editingUser ? '留空不修改' : ''">
            </label>
            <label class="field">
              <span>角色</span>
              <select v-model="form.roleKey" required>
                <option v-for="role in roleOptions" :key="role.value" :value="role.value">{{ role.label }}</option>
              </select>
            </label>
            <label class="field">
              <span>状态</span>
              <select v-model.number="form.status">
                <option :value="1">启用</option>
                <option :value="0">禁用</option>
              </select>
            </label>
          </div>

          <div v-if="message.text" :class="['message', message.type]">{{ message.text }}</div>

          <div class="dialog-actions">
            <button class="action-btn ghost" type="button" @click="closeDialog">取消</button>
            <button class="action-btn primary" type="submit" :disabled="saving || roleOptions.length === 0">
              <Save :size="17" /> 保存
            </button>
          </div>
        </form>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ChevronLeft,
  ChevronRight,
  Download,
  Pencil,
  RotateCcw,
  Save,
  Search,
  Trash2,
  Upload,
  UserPlus,
  X
} from 'lucide-vue-next'
import { userApi, type UserInfo, type UserPayload, type UserQueryParams } from '@/api/user'
import { roleApi, type RoleInfo } from '@/api/role'

const users = ref<UserInfo[]>([])
const roles = ref<RoleInfo[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogOpen = ref(false)
const editingUser = ref<UserInfo | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)

const pagination = reactive({ current: 1, size: 10 })
const filters = reactive({ username: '', nickname: '', roleKey: '', status: '' as number | '' })
const form = reactive<UserPayload>({ username: '', password: '', nickname: '', roleKey: '', status: 1 })
const message = reactive({ text: '', type: 'info' as 'info' | 'success' | 'error' })

const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pagination.size)))
const roleOptions = computed(() =>
  roles.value
    .filter(role => role.status === 1)
    .map(role => ({ label: role.roleName || role.roleKey, value: role.roleKey }))
)

const buildQueryParams = (): UserQueryParams => ({
  current: pagination.current,
  size: pagination.size,
  username: filters.username || undefined,
  nickname: filters.nickname || undefined,
  roleKey: filters.roleKey || undefined,
  status: filters.status === '' ? undefined : filters.status
})

const extractErrorMessage = (error: any, fallback: string) => {
  const responseData = error?.response?.data
  if (typeof responseData?.message === 'string' && responseData.message.trim()) return responseData.message
  if (typeof error?.message === 'string' && error.message.trim() && error.message !== '[object Object]') return error.message
  return fallback
}

const showMessage = (text: string, type: 'info' | 'success' | 'error' = 'info') => {
  message.text = text
  message.type = type
}

const loadRoles = async () => {
  try {
    const result = await roleApi.fetchRoles()
    roles.value = Array.isArray(result) ? result : []
  } catch (error) {
    console.error('Failed to load roles:', error)
    roles.value = []
  }
}

const loadUsers = async (page = pagination.current) => {
  pagination.current = page
  loading.value = true
  try {
    const result = await userApi.fetchUsers(buildQueryParams())
    users.value = result?.records || []
    total.value = Number(result?.total || 0)
  } catch (error) {
    console.error('Failed to load users:', error)
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.username = ''
  filters.nickname = ''
  filters.roleKey = ''
  filters.status = ''
  loadUsers(1)
}

const getDefaultRole = () => roleOptions.value.find(role => role.value === 'USER')?.value || roleOptions.value[0]?.value || ''

const openCreateDialog = () => {
  editingUser.value = null
  Object.assign(form, { username: '', password: '', nickname: '', roleKey: getDefaultRole(), status: 1 })
  message.text = ''
  dialogOpen.value = true
}

const openEditDialog = (user: UserInfo) => {
  editingUser.value = user
  Object.assign(form, {
    username: user.username,
    password: '',
    nickname: user.nickname || '',
    roleKey: user.roleKey || getDefaultRole(),
    status: user.status ?? 1
  })
  message.text = ''
  dialogOpen.value = true
}

const closeDialog = () => {
  dialogOpen.value = false
}

const submitUser = async () => {
  saving.value = true
  try {
    const payload = { ...form }
    if (editingUser.value && !payload.password) delete payload.password
    if (editingUser.value) {
      await userApi.updateUser(editingUser.value.id, payload)
      showMessage('用户信息已更新', 'success')
    } else {
      await userApi.createUser(payload)
      showMessage('用户已创建', 'success')
    }
    await loadUsers(editingUser.value ? pagination.current : 1)
    window.setTimeout(closeDialog, 450)
  } catch (error: any) {
    showMessage(extractErrorMessage(error, '保存用户失败'), 'error')
  } finally {
    saving.value = false
  }
}

const removeUser = async (user: UserInfo) => {
  if (!window.confirm(`确认删除用户「${user.username}」吗？`)) return
  loading.value = true
  try {
    await userApi.removeUser(user.id)
    await loadUsers(users.value.length === 1 && pagination.current > 1 ? pagination.current - 1 : pagination.current)
  } catch (error: any) {
    window.alert(extractErrorMessage(error, '删除用户失败'))
  } finally {
    loading.value = false
  }
}

const triggerImport = () => fileInputRef.value?.click()

const handleImport = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  loading.value = true
  try {
    const result = await userApi.importUsers(file)
    await loadUsers(1)
    window.alert(`导入完成：新增 ${result?.created || 0}，更新 ${result?.updated || 0}，跳过 ${result?.skipped || 0}`)
  } catch (error: any) {
    window.alert(extractErrorMessage(error, '导入用户失败'))
  } finally {
    target.value = ''
    loading.value = false
  }
}

const exportUsers = async () => {
  loading.value = true
  try {
    const blob = await userApi.exportUsers(buildQueryParams())
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `users-${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    URL.revokeObjectURL(url)
  } catch (error: any) {
    window.alert(extractErrorMessage(error, '导出用户失败'))
  } finally {
    loading.value = false
  }
}

const getRoleLabel = (roleKey?: string) => roleOptions.value.find(role => role.value === roleKey)?.label || roleKey || '-'
const getInitial = (user: UserInfo) => (user.nickname || user.username || 'U').slice(0, 1).toUpperCase()
const formatTime = (time?: string) => time ? time.replace('T', ' ').slice(0, 16) : '-'

onMounted(() => {
  loadRoles()
  loadUsers()
})
</script>

<style scoped>
.user-view { width: 100%; }
.user-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 24px; }
.eyebrow { color: var(--text-muted); font-size: 0.78rem; font-weight: 800; }
.page-title { margin-top: 4px; color: var(--text); font-size: 1.8rem; font-weight: 900; }
.header-actions, .filter-actions, .row-actions, .dialog-actions, .pager { display: flex; align-items: center; gap: 10px; }
.file-input { display: none; }
.filter-panel { display: grid; grid-template-columns: repeat(4, minmax(150px, 1fr)) auto; gap: 14px; align-items: end; padding: 18px; border: 1px solid var(--border); border-radius: 8px; background: var(--surface); box-shadow: var(--card-shadow); margin-bottom: 18px; }
.field { display: flex; flex-direction: column; gap: 7px; color: var(--text-muted); font-size: 0.78rem; font-weight: 800; }
.field input, .field select { height: 40px; border: 1px solid var(--border); border-radius: 8px; padding: 0 12px; outline: none; background: var(--surface-hover); color: var(--text); font-size: 0.9rem; }
.field input:focus, .field select:focus { border-color: var(--accent); box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12); }
.action-btn, .icon-text-btn, .icon-btn, .plain-icon, .pager button { display: inline-flex; align-items: center; justify-content: center; border: none; cursor: pointer; color: var(--text); }
.action-btn, .icon-text-btn { height: 40px; gap: 8px; padding: 0 14px; border-radius: 8px; font-weight: 800; }
.action-btn.primary, .icon-text-btn { color: #fff; background: var(--accent); }
.action-btn.ghost, .icon-text-btn.muted { color: var(--text); background: var(--surface); border: 1px solid var(--border); }
.action-btn:disabled, .icon-text-btn:disabled, .pager button:disabled { opacity: 0.5; cursor: not-allowed; }
.table-shell { overflow: auto; border: 1px solid var(--border); border-radius: 8px; background: var(--surface); box-shadow: var(--card-shadow); }
.user-table { width: 100%; min-width: 820px; border-collapse: collapse; }
.user-table th, .user-table td { padding: 15px 16px; border-bottom: 1px solid var(--border); text-align: left; color: var(--text); font-size: 0.9rem; }
.user-table th { color: var(--text-muted); background: var(--surface-hover); font-size: 0.78rem; font-weight: 900; }
.user-table tr:last-child td { border-bottom: none; }
.actions-col { width: 112px; }
.user-name { display: flex; align-items: center; gap: 10px; font-weight: 800; }
.avatar { width: 32px; height: 32px; display: inline-flex; align-items: center; justify-content: center; border-radius: 8px; color: #fff; background: #0f766e; font-size: 0.82rem; }
.role-chip, .status-pill { display: inline-flex; align-items: center; min-height: 26px; padding: 0 9px; border-radius: 999px; font-size: 0.78rem; font-weight: 800; }
.role-chip { color: #1d4ed8; background: rgba(37, 99, 235, 0.1); }
.status-pill.enabled { color: #15803d; background: rgba(34, 197, 94, 0.12); }
.status-pill.disabled { color: #b91c1c; background: rgba(239, 68, 68, 0.12); }
.icon-btn, .plain-icon, .pager button { width: 34px; height: 34px; border-radius: 8px; background: var(--surface-hover); }
.icon-btn.danger { color: #dc2626; background: rgba(220, 38, 38, 0.08); }
.empty-cell { height: 180px; text-align: center !important; color: var(--text-muted) !important; }
.pagination-bar { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; color: var(--text-muted); font-weight: 800; font-size: 0.86rem; }
.pager span { min-width: 72px; text-align: center; }
.dialog-overlay { position: fixed; inset: 0; z-index: 220; display: flex; align-items: center; justify-content: center; padding: 24px; background: rgba(15, 23, 42, 0.24); backdrop-filter: blur(4px); }
.dialog-panel { width: min(620px, 100%); padding: 22px; border-radius: 8px; border: 1px solid var(--border); background: var(--surface); box-shadow: 0 24px 80px rgba(15, 23, 42, 0.22); }
.dialog-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px; }
.dialog-header h2 { color: var(--text); font-size: 1.2rem; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.message { margin-top: 16px; padding: 10px 12px; border-radius: 8px; font-weight: 800; font-size: 0.86rem; }
.message.success { color: #15803d; background: rgba(34, 197, 94, 0.12); }
.message.error { color: #b91c1c; background: rgba(239, 68, 68, 0.12); }
.dialog-actions { justify-content: flex-end; margin-top: 20px; }
.dialog-fade-enter-active, .dialog-fade-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.dialog-fade-enter-from, .dialog-fade-leave-to { opacity: 0; transform: scale(0.98); }
@media (max-width: 980px) { .user-header { align-items: stretch; flex-direction: column; } .header-actions { flex-wrap: wrap; } .filter-panel { grid-template-columns: repeat(2, minmax(0, 1fr)); } .filter-actions { grid-column: 1 / -1; } }
@media (max-width: 620px) { .filter-panel, .form-grid { grid-template-columns: 1fr; } }
</style>
