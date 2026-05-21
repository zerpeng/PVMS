<template>
  <aside :class="['details-panel', { open: mediaStore.isDetailPanelOpen }]">
    <div class="close-btn" @click="mediaStore.closeDetailPanel">
      <X :size="18" />
    </div>

    <div v-if="mediaStore.selectedAsset" class="details-content">
      <div class="preview-box" :style="{ background: `linear-gradient(45deg, ${mediaStore.selectedAsset.thumbColor}, var(--bg))` }">
        <div class="preview-inner">
          <ImageIcon v-if="mediaStore.selectedAsset.mediaType !== 2" :size="34" />
          <Film v-else :size="34" />
        </div>
      </div>

      <div class="info-section">
        <h3 class="detail-title">{{ mediaStore.selectedAsset.name }}</h3>
        <div class="badge-group">
          <span class="type-badge">{{ mediaStore.selectedAsset.type }}</span>
          <span class="rating-badge">{{ mediaStore.selectedAsset.mediaType === 2 ? '视频' : '图片' }}</span>
        </div>
      </div>

      <div v-if="!authInfo.visitor" class="quick-actions">
        <button class="primary-action" type="button" @click="openAssignDialog('album')">
          <FolderPlus :size="16" /> 添加到相册
        </button>
        <button class="secondary-action" type="button" @click="openAssignDialog('tag')">
          <Tags :size="16" /> 添加到标签
        </button>
      </div>

      <div class="meta-group">
        <h5 class="group-header"><Camera :size="14" /> 相机元数据</h5>
        <div class="meta-rows">
          <div class="meta-row">
            <span class="label">相机型号</span>
            <span class="value">{{ mediaStore.selectedAsset.metadata?.camera || '未知' }}</span>
          </div>
          <div class="meta-row">
            <span class="label">曝光参数</span>
            <span class="value">{{ mediaStore.selectedAsset.metadata?.exposure || '无数据' }}</span>
          </div>
          <div class="meta-row">
            <span class="label">分辨率</span>
            <span class="value">{{ mediaStore.selectedAsset.metadata?.dimensions || '无数据' }}</span>
          </div>
          <div class="meta-row">
            <span class="label">文件大小</span>
            <span class="value">{{ mediaStore.selectedAsset.metadata?.size || '未知' }}</span>
          </div>
        </div>
      </div>

      <button class="download-btn" type="button">
        <Download :size="18" /> 下载原始文件
      </button>
    </div>

    <transition name="dialog-fade">
      <div v-if="assignDialog.visible" class="dialog-overlay" @click.self="closeAssignDialog">
        <div class="dialog-card">
          <h2>{{ assignDialog.type === 'album' ? '添加到相册' : '添加到标签' }}</h2>
          <p class="dialog-text">将当前媒体添加到{{ assignDialog.type === 'album' ? '相册' : '标签' }}。</p>

          <div v-if="assignOptions.length > 0" class="target-list">
            <label
              v-for="item in assignOptions"
              :key="item.id"
              :class="['target-item', { active: assignDialog.targetId === item.id }]"
            >
              <input v-model="assignDialog.targetId" type="radio" :value="item.id">
              <div>
                <strong>{{ item.name }}</strong>
                <p>{{ item.description }}</p>
              </div>
            </label>
          </div>
          <div v-else class="empty-state">
            暂无可用{{ assignDialog.type === 'album' ? '相册' : '标签' }}，请先到“相册标签”页面创建。
          </div>

          <div class="dialog-actions">
            <button class="secondary-action" type="button" @click="closeAssignDialog">取消</button>
            <button
              class="primary-action"
              type="button"
              :disabled="loading || !assignDialog.targetId || assignOptions.length === 0"
              @click="submitAssign"
            >
              确认添加
            </button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="dialog-fade">
      <div v-if="messageDialog.visible" class="dialog-overlay" @click.self="messageDialog.visible = false">
        <div class="dialog-card message-dialog" :class="`message-${messageDialog.type}`">
          <h2>{{ messageDialog.title }}</h2>
          <p class="dialog-text">{{ messageDialog.message }}</p>
          <div class="dialog-actions">
            <button class="primary-action" type="button" @click="messageDialog.visible = false">知道了</button>
          </div>
        </div>
      </div>
    </transition>
  </aside>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { X, Camera, Download, FolderPlus, Tags, ImageIcon, Film } from 'lucide-vue-next'
import { mediaApi } from '@/api/media'
import { useMediaStore } from '@/stores/media'
import { getAuthInfo } from '@/utils/authz'

type AssignType = 'album' | 'tag'
type MessageType = 'info' | 'success' | 'error'

interface AlbumItem {
  id: number
  name: string
  description?: string | null
}

interface TagItem {
  id: number
  name: string
  category?: string | null
}

const mediaStore = useMediaStore()
const authInfo = computed(() => getAuthInfo())
const loading = ref(false)
const albums = ref<AlbumItem[]>([])
const tags = ref<TagItem[]>([])

const assignDialog = reactive({
  visible: false,
  type: 'album' as AssignType,
  targetId: null as number | null,
})

const messageDialog = reactive({
  visible: false,
  title: '',
  message: '',
  type: 'info' as MessageType,
})

const assignOptions = computed(() => {
  if (assignDialog.type === 'album') {
    return albums.value.map(item => ({
      id: item.id,
      name: item.name,
      description: item.description || '暂无相册描述',
    }))
  }

  return tags.value.map(item => ({
    id: item.id,
    name: item.name,
    description: item.category || '未设置标签分类',
  }))
})

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

const loadReferenceData = async () => {
  const [albumData, tagData] = await Promise.all([
    mediaApi.fetchAlbums(),
    mediaApi.fetchTags(),
  ])
  albums.value = Array.isArray(albumData) ? albumData : []
  tags.value = Array.isArray(tagData) ? tagData : []
}

const openAssignDialog = async (type: AssignType) => {
  if (!mediaStore.selectedAsset) return
  await loadReferenceData()
  assignDialog.type = type
  assignDialog.targetId = null
  assignDialog.visible = true
}

const closeAssignDialog = () => {
  assignDialog.visible = false
}

const showMessage = (title: string, message: string, type: MessageType) => {
  messageDialog.title = title
  messageDialog.message = message
  messageDialog.type = type
  messageDialog.visible = true
}

const submitAssign = async () => {
  if (!assignDialog.targetId || !mediaStore.selectedAsset) return

  loading.value = true
  try {
    if (assignDialog.type === 'album') {
      await mediaApi.addMediaToAlbum(assignDialog.targetId, mediaStore.selectedAsset.id)
    } else {
      await mediaApi.bindTagToMedia(mediaStore.selectedAsset.id, assignDialog.targetId)
    }

    closeAssignDialog()
    showMessage('添加成功', `已将当前媒体添加到${assignDialog.type === 'album' ? '相册' : '标签'}`, 'success')
  } catch (error: any) {
    showMessage('添加失败', extractErrorMessage(error, '添加媒体关系失败'), 'error')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.details-panel {
  width: 420px;
  background: var(--surface);
  border-left: 1px solid var(--border);
  position: absolute;
  top: 0;
  bottom: 0;
  right: 0;
  transform: translateX(100%);
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  z-index: 100;
  display: flex;
  flex-direction: column;
  box-shadow: -20px 0 50px rgba(0, 0, 0, 0.05);
}

.details-panel.open {
  transform: translateX(0);
}

.close-btn {
  position: absolute;
  right: 20px;
  top: 20px;
  width: 32px;
  height: 32px;
  background: var(--surface-hover);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 110;
  color: var(--text);
}

.details-content {
  padding: 2.5rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.preview-box {
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: 24px;
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-inner {
  width: 76px;
  height: 76px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text);
}

.detail-title {
  font-size: 1.25rem;
  margin-bottom: 0.5rem;
  color: var(--text);
}

.badge-group {
  display: flex;
  gap: 8px;
}

.type-badge {
  padding: 4px 8px;
  background: var(--accent);
  color: white;
  border-radius: 8px;
  font-size: 0.7rem;
  font-weight: 600;
}

.rating-badge {
  padding: 4px 8px;
  background: var(--surface-hover);
  border-radius: 8px;
  font-size: 0.7rem;
  color: var(--text-muted);
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.meta-rows {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 0.85rem;
}

.meta-row .label {
  color: var(--text-muted);
}

.meta-row .value {
  color: var(--text);
  font-weight: 500;
  text-align: right;
}

.primary-action,
.secondary-action,
.download-btn {
  width: 100%;
  padding: 1rem;
  border-radius: 18px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: opacity 0.2s;
}

.primary-action {
  border: none;
  background: var(--accent);
  color: white;
}

.secondary-action {
  border: 1px solid var(--border);
  background: var(--surface-hover);
  color: var(--text);
}

.download-btn {
  margin-top: auto;
  border: none;
  background: #0f172a;
  color: white;
}

.primary-action:disabled {
  opacity: 0.45;
  cursor: not-allowed;
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
  font-size: 1.2rem;
}

.dialog-text {
  margin-top: 10px;
  color: var(--text-muted);
  line-height: 1.6;
}

.target-list {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 320px;
  overflow-y: auto;
}

.target-item {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--surface-hover);
  cursor: pointer;
}

.target-item.active {
  border-color: var(--accent);
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.12);
}

.target-item input {
  margin-top: 4px;
}

.target-item strong {
  color: var(--text);
  display: block;
}

.target-item p {
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 0.82rem;
}

.empty-state {
  margin-top: 20px;
  padding: 24px;
  border-radius: 16px;
  background: var(--surface-hover);
  color: var(--text-muted);
  line-height: 1.6;
}

.dialog-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.message-dialog {
  width: min(420px, 100%);
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
</style>
