<template>
  <div class="role-view">
    <header class="role-header">
      <div>
        <p class="eyebrow">ACCESS CONTROL</p>
        <h1 class="page-title">角色管理</h1>
      </div>
      <button class="primary-btn" type="button" @click="openCreateDialog">
        <ShieldPlus :size="17" />
        新增角色
      </button>
    </header>

    <section class="role-grid">
      <article v-for="role in roles" :key="role.id" class="role-card">
        <div class="role-card-head">
          <div>
            <h3>{{ role.roleName }}</h3>
            <p>{{ role.roleKey }}</p>
          </div>
          <span :class="['status-pill', role.status === 1 ? 'on' : 'off']">{{ role.status === 1 ? '启用' : '禁用' }}</span>
        </div>
        <div class="permission-summary">
          <span>{{ role.superAdmin ? '全部目录' : `${role.directoryIds.length} 个目录` }}</span>
          <span>{{ role.superAdmin ? '全部相册' : `${role.albumIds.length} 个相册` }}</span>
          <span>{{ role.superAdmin ? '全部标签' : `${role.tagIds.length} 个标签` }}</span>
        </div>
        <p class="role-remark">{{ role.remark || (role.visitor ? '访客只允许查看授权媒体库和相册标签。' : '按配置权限访问内容。') }}</p>
        <div class="card-actions">
          <button class="ghost-btn" type="button" @click="openEditDialog(role)">编辑</button>
          <button class="danger-btn" type="button" :disabled="role.builtin" @click="removeRole(role)">删除</button>
        </div>
      </article>
    </section>

    <transition name="dialog-fade">
      <div v-if="dialogOpen" class="dialog-overlay" @click.self="closeDialog">
        <form class="dialog-panel" @submit.prevent="submitRole">
          <header class="dialog-header">
            <h2>{{ editingRole ? '修改角色' : '新增角色' }}</h2>
            <button class="icon-btn" type="button" @click="closeDialog"><X :size="18" /></button>
          </header>

          <div class="form-grid">
            <label class="field">
              <span>角色标识</span>
              <input v-model.trim="form.roleKey" type="text" :disabled="Boolean(editingRole?.builtin)" required>
            </label>
            <label class="field">
              <span>角色名称</span>
              <input v-model.trim="form.roleName" type="text" required>
            </label>
            <label class="field">
              <span>状态</span>
              <select v-model.number="form.status">
                <option :value="1">启用</option>
                <option :value="0">禁用</option>
              </select>
            </label>
            <label class="field">
              <span>角色类型</span>
              <select v-model="roleMode">
                <option value="normal">普通角色</option>
                <option value="visitor">访客</option>
                <option value="admin">超管</option>
              </select>
            </label>
          </div>

          <label class="field">
            <span>备注</span>
            <textarea v-model.trim="form.remark" rows="2"></textarea>
          </label>

          <section class="permission-section">
            <h3>菜单权限</h3>
            <div class="check-grid compact">
              <label v-for="menu in menuOptions" :key="menu.value" class="check-item">
                <input v-model="form.menuKeys" type="checkbox" :value="menu.value" :disabled="form.superAdmin || form.visitor">
                <span>{{ menu.label }}</span>
              </label>
            </div>
          </section>

          <section class="permission-section">
            <h3>目录权限</h3>
            <div class="check-grid">
              <label v-for="directory in directories" :key="directory.id" class="check-item">
                <input v-model="form.directoryIds" type="checkbox" :value="directory.id" :disabled="form.superAdmin">
                <span>{{ getDirectoryName(directory.dirPath) }}</span>
              </label>
            </div>
          </section>

          <section class="permission-section">
            <h3>相册权限</h3>
            <div class="check-grid">
              <label v-for="album in albums" :key="album.id" class="check-item">
                <input v-model="form.albumIds" type="checkbox" :value="album.id" :disabled="form.superAdmin">
                <span>{{ album.name }}</span>
              </label>
            </div>
          </section>

          <section class="permission-section">
            <h3>标签权限</h3>
            <div class="check-grid">
              <label v-for="tag in tags" :key="tag.id" class="check-item">
                <input v-model="form.tagIds" type="checkbox" :value="tag.id" :disabled="form.superAdmin">
                <span>{{ tag.name }}</span>
              </label>
            </div>
          </section>

          <div v-if="message" class="message">{{ message }}</div>
          <div class="dialog-actions">
            <button class="ghost-btn" type="button" @click="closeDialog">取消</button>
            <button class="primary-btn" type="submit" :disabled="saving">保存</button>
          </div>
        </form>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ShieldPlus, X } from 'lucide-vue-next'
import { roleApi, type RoleInfo, type RolePayload } from '@/api/role'
import { mediaApi } from '@/api/media'

const menuOptions = [
  { label: '媒体库', value: 'library' },
  { label: '项目工程', value: 'projects' },
  { label: '相册标签', value: 'album-tags' },
  { label: '我的收藏', value: 'favorites' },
  { label: '用户管理', value: 'users' },
  { label: '角色管理', value: 'roles' }
]

const roles = ref<RoleInfo[]>([])
const directories = ref<any[]>([])
const albums = ref<any[]>([])
const tags = ref<any[]>([])
const dialogOpen = ref(false)
const saving = ref(false)
const message = ref('')
const editingRole = ref<RoleInfo | null>(null)
const roleMode = ref<'normal' | 'visitor' | 'admin'>('normal')

const form = reactive<RolePayload>({
  roleKey: '',
  roleName: '',
  status: 1,
  superAdmin: false,
  visitor: false,
  menuKeys: ['library', 'projects', 'album-tags', 'favorites'],
  directoryIds: [],
  albumIds: [],
  tagIds: [],
  remark: ''
})

watch(roleMode, (mode) => {
  form.superAdmin = mode === 'admin'
  form.visitor = mode === 'visitor'
  if (form.superAdmin) form.menuKeys = menuOptions.map(item => item.value)
  if (form.visitor) form.menuKeys = ['library', 'album-tags']
})

const loadData = async () => {
  const [roleData, directoryData, albumData, tagData] = await Promise.all([
    roleApi.fetchRoles(),
    mediaApi.fetchDirectories(),
    mediaApi.fetchAlbums(),
    mediaApi.fetchTags()
  ])
  roles.value = Array.isArray(roleData) ? roleData : []
  directories.value = Array.isArray(directoryData) ? directoryData : []
  albums.value = Array.isArray(albumData) ? albumData : []
  tags.value = Array.isArray(tagData) ? tagData : []
}

const resetForm = () => {
  Object.assign(form, {
    roleKey: '',
    roleName: '',
    status: 1,
    superAdmin: false,
    visitor: false,
    menuKeys: ['library', 'projects', 'album-tags', 'favorites'],
    directoryIds: [],
    albumIds: [],
    tagIds: [],
    remark: ''
  })
  roleMode.value = 'normal'
  message.value = ''
}

const openCreateDialog = () => {
  editingRole.value = null
  resetForm()
  dialogOpen.value = true
}

const openEditDialog = (role: RoleInfo) => {
  editingRole.value = role
  Object.assign(form, {
    roleKey: role.roleKey,
    roleName: role.roleName,
    status: role.status,
    superAdmin: role.superAdmin,
    visitor: role.visitor,
    menuKeys: [...(role.menuKeys || [])],
    directoryIds: [...(role.directoryIds || [])],
    albumIds: [...(role.albumIds || [])],
    tagIds: [...(role.tagIds || [])],
    remark: role.remark || ''
  })
  roleMode.value = role.superAdmin ? 'admin' : role.visitor ? 'visitor' : 'normal'
  message.value = ''
  dialogOpen.value = true
}

const closeDialog = () => {
  dialogOpen.value = false
}

const submitRole = async () => {
  saving.value = true
  try {
    if (editingRole.value) {
      await roleApi.updateRole(editingRole.value.id, form)
    } else {
      await roleApi.createRole(form)
    }
    await loadData()
    closeDialog()
  } catch (error: any) {
    message.value = error?.response?.data?.message || error?.message || '保存角色失败'
  } finally {
    saving.value = false
  }
}

const removeRole = async (role: RoleInfo) => {
  if (role.builtin || !window.confirm(`确认删除角色「${role.roleName}」吗？`)) return
  await roleApi.removeRole(role.id)
  await loadData()
}

const getDirectoryName = (path: string) => path?.replace(/\\/g, '/').split('/').filter(Boolean).pop() || path

onMounted(loadData)
</script>

<style scoped>
.role-view { width: 100%; }
.role-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 24px; }
.eyebrow { color: var(--text-muted); font-size: 0.78rem; font-weight: 800; }
.page-title { margin-top: 4px; color: var(--text); font-size: 1.8rem; font-weight: 900; }
.role-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 18px; }
.role-card { padding: 20px; border: 1px solid var(--border); border-radius: 8px; background: var(--surface); box-shadow: var(--card-shadow); }
.role-card-head { display: flex; justify-content: space-between; gap: 14px; }
.role-card h3 { color: var(--text); font-size: 1.05rem; }
.role-card p { color: var(--text-muted); margin-top: 6px; font-size: 0.84rem; }
.status-pill { height: 26px; padding: 0 9px; border-radius: 999px; font-size: 0.78rem; font-weight: 800; display: inline-flex; align-items: center; }
.status-pill.on { color: #15803d; background: rgba(34,197,94,.12); }
.status-pill.off { color: #b91c1c; background: rgba(239,68,68,.12); }
.permission-summary { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 18px; }
.permission-summary span { padding: 6px 9px; border-radius: 999px; color: #1d4ed8; background: rgba(37,99,235,.1); font-weight: 800; font-size: .78rem; }
.role-remark { min-height: 38px; }
.card-actions, .dialog-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 18px; }
.primary-btn, .ghost-btn, .danger-btn, .icon-btn { height: 40px; display: inline-flex; align-items: center; justify-content: center; gap: 8px; border-radius: 8px; font-weight: 800; cursor: pointer; padding: 0 14px; }
.primary-btn { border: none; color: #fff; background: var(--accent); }
.ghost-btn { color: var(--text); background: var(--surface); border: 1px solid var(--border); }
.danger-btn { color: #dc2626; border: none; background: rgba(220,38,38,.08); }
.danger-btn:disabled, .primary-btn:disabled { opacity: .45; cursor: not-allowed; }
.icon-btn { width: 36px; padding: 0; border: none; color: var(--text); background: var(--surface-hover); }
.dialog-overlay { position: fixed; inset: 0; z-index: 240; display: flex; align-items: center; justify-content: center; padding: 24px; background: rgba(15,23,42,.24); backdrop-filter: blur(4px); }
.dialog-panel { width: min(820px, 100%); max-height: 90vh; overflow: auto; padding: 22px; border-radius: 8px; background: var(--surface); border: 1px solid var(--border); box-shadow: 0 24px 80px rgba(15,23,42,.22); }
.dialog-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.dialog-header h2, .permission-section h3 { color: var(--text); }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.field { display: flex; flex-direction: column; gap: 7px; margin-top: 12px; color: var(--text-muted); font-size: .78rem; font-weight: 800; }
.field input, .field select, .field textarea { border: 1px solid var(--border); border-radius: 8px; padding: 10px 12px; background: var(--surface-hover); color: var(--text); outline: none; }
.permission-section { margin-top: 18px; }
.check-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 10px; margin-top: 10px; max-height: 190px; overflow: auto; }
.check-grid.compact { max-height: none; }
.check-item { display: flex; align-items: center; gap: 8px; padding: 10px; border: 1px solid var(--border); border-radius: 8px; background: var(--surface-hover); color: var(--text); font-weight: 700; font-size: .86rem; }
.message { margin-top: 14px; color: #b91c1c; font-weight: 800; }
.dialog-fade-enter-active, .dialog-fade-leave-active { transition: opacity .2s ease, transform .2s ease; }
.dialog-fade-enter-from, .dialog-fade-leave-to { opacity: 0; transform: scale(.98); }
@media (max-width: 720px) { .role-header, .form-grid { display: flex; flex-direction: column; align-items: stretch; } }
</style>
