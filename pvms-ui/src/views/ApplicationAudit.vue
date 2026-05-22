<template>
  <div class="audit-view">
    <header class="audit-header">
      <div>
        <p class="eyebrow">ACCESS REQUESTS</p>
        <h1 class="page-title">访问审核</h1>
      </div>
      <div class="toolbar">
        <select v-model="statusFilter" @change="loadApplications">
          <option value="">全部申请</option>
          <option :value="0">待审核</option>
          <option :value="1">已通过</option>
          <option :value="2">已拒绝</option>
        </select>
        <button class="ghost-btn" type="button" @click="loadApplications">
          <RefreshCcw :size="16" />
          刷新
        </button>
      </div>
    </header>

    <section class="summary-strip">
      <div>
        <span>待审核</span>
        <strong>{{ pendingCount }}</strong>
      </div>
      <div>
        <span>总申请</span>
        <strong>{{ applications.length }}</strong>
      </div>
    </section>

    <section class="audit-table">
      <div class="table-row table-head">
        <span>申请账号</span>
        <span>申请人</span>
        <span>申请理由</span>
        <span>状态</span>
        <span>申请时间</span>
        <span>操作</span>
      </div>
      <div v-if="loading" class="empty-state">加载中...</div>
      <div v-else-if="applications.length === 0" class="empty-state">暂无访问申请</div>
      <template v-else>
        <div v-for="item in applications" :key="item.id" class="table-row">
          <span class="strong">{{ item.username }}</span>
          <span>{{ item.applicantName || '-' }}</span>
          <span class="reason">{{ item.reason || '-' }}</span>
          <span>
            <b :class="['status-pill', statusClass(item.status)]">{{ item.statusText }}</b>
          </span>
          <span>{{ formatTime(item.createTime) }}</span>
          <span class="row-actions">
            <button class="primary-btn small" type="button" :disabled="item.status !== 0" @click="openAuditDialog(item)">
              审核
            </button>
          </span>
        </div>
      </template>
    </section>

    <transition name="dialog-fade">
      <div v-if="dialogOpen" class="dialog-overlay" @click.self="closeDialog">
        <form class="dialog-panel" @submit.prevent="submitAudit">
          <header class="dialog-header">
            <div>
              <h2>审核访问申请</h2>
              <p>{{ currentApplication?.username }} / {{ currentApplication?.applicantName }}</p>
            </div>
            <button class="icon-btn" type="button" @click="closeDialog"><X :size="18" /></button>
          </header>

          <div class="decision-tabs">
            <button type="button" :class="{ active: form.approved }" @click="form.approved = true">通过</button>
            <button type="button" :class="{ active: !form.approved }" @click="form.approved = false">拒绝</button>
          </div>

          <template v-if="form.approved">
            <div class="form-grid">
              <label class="field">
                <span>角色名称</span>
                <input v-model.trim="form.roleName" type="text" required>
              </label>
              <label class="field">
                <span>角色类型</span>
                <select v-model="roleMode">
                  <option value="visitor">访客</option>
                  <option value="normal">普通用户</option>
                  <option value="admin">超管</option>
                </select>
              </label>
            </div>

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
          </template>

          <label class="field">
            <span>审核备注</span>
            <textarea v-model.trim="form.remark" rows="3" placeholder="可填写通过说明或拒绝原因"></textarea>
          </label>

          <div v-if="message" class="message">{{ message }}</div>
          <div class="dialog-actions">
            <button class="ghost-btn" type="button" @click="closeDialog">取消</button>
            <button class="primary-btn" type="submit" :disabled="saving">提交审核</button>
          </div>
        </form>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RefreshCcw, X } from 'lucide-vue-next'
import { applicationApi, type AuditPayload, type VisitorApplication } from '@/api/application'
import { mediaApi } from '@/api/media'

const menuOptions = [
  { label: '媒体库', value: 'library' },
  { label: '项目工程', value: 'projects' },
  { label: '相册标签', value: 'album-tags' },
  { label: '我的收藏', value: 'favorites' },
  { label: '用户管理', value: 'users' },
  { label: '角色管理', value: 'roles' },
  { label: '访问审核', value: 'applications' }
]

const applications = ref<VisitorApplication[]>([])
const directories = ref<any[]>([])
const albums = ref<any[]>([])
const tags = ref<any[]>([])
const statusFilter = ref<number | ''>(0)
const loading = ref(false)
const saving = ref(false)
const dialogOpen = ref(false)
const message = ref('')
const currentApplication = ref<VisitorApplication | null>(null)
const roleMode = ref<'visitor' | 'normal' | 'admin'>('visitor')

const form = reactive<AuditPayload>({
  approved: true,
  roleName: '',
  superAdmin: false,
  visitor: true,
  menuKeys: ['library', 'album-tags'],
  directoryIds: [],
  albumIds: [],
  tagIds: [],
  remark: ''
})

const pendingCount = computed(() => applications.value.filter(item => item.status === 0).length)

watch(roleMode, (mode) => {
  form.superAdmin = mode === 'admin'
  form.visitor = mode === 'visitor'
  if (form.superAdmin) {
    form.menuKeys = menuOptions.map(item => item.value)
  } else if (form.visitor) {
    form.menuKeys = ['library', 'album-tags']
  } else if (!form.menuKeys.length || form.menuKeys.every(key => ['library', 'album-tags'].includes(key))) {
    form.menuKeys = ['library', 'projects', 'album-tags', 'favorites']
  }
})

const loadApplications = async () => {
  loading.value = true
  try {
    applications.value = await applicationApi.fetchApplications(statusFilter.value)
  } finally {
    loading.value = false
  }
}

const loadPermissionOptions = async () => {
  const [directoryData, albumData, tagData] = await Promise.all([
    mediaApi.fetchDirectories(),
    mediaApi.fetchAlbums(),
    mediaApi.fetchTags()
  ])
  directories.value = Array.isArray(directoryData) ? directoryData : []
  albums.value = Array.isArray(albumData) ? albumData : []
  tags.value = Array.isArray(tagData) ? tagData : []
}

const openAuditDialog = async (application: VisitorApplication) => {
  currentApplication.value = application
  Object.assign(form, {
    approved: true,
    roleName: `${application.applicantName || application.username}访问权限`,
    superAdmin: false,
    visitor: true,
    menuKeys: ['library', 'album-tags'],
    directoryIds: [],
    albumIds: [],
    tagIds: [],
    remark: ''
  })
  roleMode.value = 'visitor'
  message.value = ''
  dialogOpen.value = true
  if (directories.value.length === 0 && albums.value.length === 0 && tags.value.length === 0) {
    await loadPermissionOptions()
  }
}

const closeDialog = () => {
  dialogOpen.value = false
}

const submitAudit = async () => {
  if (!currentApplication.value) return
  saving.value = true
  message.value = ''
  try {
    await applicationApi.auditApplication(currentApplication.value.id, { ...form })
    await loadApplications()
    closeDialog()
  } catch (error: any) {
    message.value = error?.response?.data?.message || error?.message || '审核失败'
  } finally {
    saving.value = false
  }
}

const statusClass = (status: number) => {
  if (status === 1) return 'approved'
  if (status === 2) return 'rejected'
  return 'pending'
}

const formatTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

const getDirectoryName = (path: string) => path?.replace(/\\/g, '/').split('/').filter(Boolean).pop() || path

onMounted(() => {
  loadApplications()
})
</script>

<style scoped>
.audit-view { width: 100%; }
.audit-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 20px; }
.eyebrow { color: var(--text-muted); font-size: 0.78rem; font-weight: 800; }
.page-title { margin-top: 4px; color: var(--text); font-size: 1.8rem; font-weight: 900; }
.toolbar { display: flex; gap: 10px; align-items: center; }
.toolbar select { height: 40px; border: 1px solid var(--border); border-radius: 8px; background: var(--surface); color: var(--text); padding: 0 12px; font-weight: 700; }
.summary-strip { display: grid; grid-template-columns: repeat(2, minmax(160px, 1fr)); gap: 12px; margin-bottom: 18px; }
.summary-strip div { padding: 16px 18px; border: 1px solid var(--border); border-radius: 8px; background: var(--surface); }
.summary-strip span { display: block; color: var(--text-muted); font-size: .78rem; font-weight: 800; }
.summary-strip strong { display: block; margin-top: 6px; color: var(--text); font-size: 1.5rem; }
.audit-table { border: 1px solid var(--border); border-radius: 8px; overflow: hidden; background: var(--surface); }
.table-row { display: grid; grid-template-columns: 1fr 1fr minmax(180px, 1.5fr) 110px 150px 90px; align-items: center; gap: 14px; min-height: 58px; padding: 0 16px; border-top: 1px solid var(--border); color: var(--text-muted); font-size: .88rem; }
.table-row:first-child { border-top: none; }
.table-head { min-height: 44px; background: var(--surface-hover); color: var(--text); font-weight: 900; font-size: .78rem; }
.strong { color: var(--text); font-weight: 800; }
.reason { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.row-actions { display: flex; justify-content: flex-end; }
.status-pill { display: inline-flex; align-items: center; height: 26px; padding: 0 9px; border-radius: 999px; font-size: .76rem; font-weight: 900; }
.status-pill.pending { color: #b45309; background: rgba(245,158,11,.13); }
.status-pill.approved { color: #15803d; background: rgba(34,197,94,.12); }
.status-pill.rejected { color: #b91c1c; background: rgba(239,68,68,.12); }
.empty-state { padding: 36px; text-align: center; color: var(--text-muted); font-weight: 800; border-top: 1px solid var(--border); }
.primary-btn, .ghost-btn, .icon-btn { height: 40px; display: inline-flex; align-items: center; justify-content: center; gap: 8px; border-radius: 8px; font-weight: 800; cursor: pointer; padding: 0 14px; }
.primary-btn { border: none; color: #fff; background: var(--accent); }
.primary-btn.small { height: 32px; padding: 0 12px; font-size: .8rem; }
.primary-btn:disabled { opacity: .45; cursor: not-allowed; }
.ghost-btn { color: var(--text); background: var(--surface); border: 1px solid var(--border); }
.icon-btn { width: 36px; padding: 0; border: none; color: var(--text); background: var(--surface-hover); }
.dialog-overlay { position: fixed; inset: 0; z-index: 240; display: flex; align-items: center; justify-content: center; padding: 24px; background: rgba(15,23,42,.24); backdrop-filter: blur(4px); }
.dialog-panel { width: min(880px, 100%); max-height: 90vh; overflow: auto; padding: 22px; border-radius: 8px; background: var(--surface); border: 1px solid var(--border); box-shadow: 0 24px 80px rgba(15,23,42,.22); }
.dialog-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 16px; }
.dialog-header h2, .permission-section h3 { color: var(--text); }
.dialog-header p { margin-top: 5px; color: var(--text-muted); font-weight: 700; }
.decision-tabs { display: inline-flex; padding: 4px; border: 1px solid var(--border); border-radius: 8px; background: var(--surface-hover); }
.decision-tabs button { height: 34px; min-width: 76px; border: none; border-radius: 6px; background: transparent; color: var(--text-muted); font-weight: 900; cursor: pointer; }
.decision-tabs button.active { background: var(--accent); color: #fff; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.field { display: flex; flex-direction: column; gap: 7px; margin-top: 14px; color: var(--text-muted); font-size: .78rem; font-weight: 800; }
.field input, .field select, .field textarea { border: 1px solid var(--border); border-radius: 8px; padding: 10px 12px; background: var(--surface-hover); color: var(--text); outline: none; }
.permission-section { margin-top: 18px; }
.check-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 10px; margin-top: 10px; max-height: 180px; overflow: auto; }
.check-grid.compact { max-height: none; }
.check-item { display: flex; align-items: center; gap: 8px; padding: 10px; border: 1px solid var(--border); border-radius: 8px; background: var(--surface-hover); color: var(--text); font-weight: 700; font-size: .86rem; min-width: 0; }
.check-item span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.message { margin-top: 14px; color: #b91c1c; font-weight: 800; }
.dialog-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 18px; }
.dialog-fade-enter-active, .dialog-fade-leave-active { transition: opacity .2s ease, transform .2s ease; }
.dialog-fade-enter-from, .dialog-fade-leave-to { opacity: 0; transform: scale(.98); }
@media (max-width: 900px) {
  .audit-header, .toolbar, .form-grid { display: flex; flex-direction: column; align-items: stretch; }
  .summary-strip { grid-template-columns: 1fr; }
  .audit-table { overflow-x: auto; }
  .table-row { min-width: 860px; }
}
</style>
