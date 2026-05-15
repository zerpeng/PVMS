<template>
  <div class="favorites-view detail-mode">
    <section class="media-section">
      <div class="media-header">
        <div>
          <p class="media-kicker">个人媒体</p>
          <h2 class="media-title">我的收藏</h2>
          <p class="media-subtitle">
            <template v-if="selectedIds.length > 0">
              已选中 {{ selectedIds.length }} 项
            </template>
            <template v-else>
              共 {{ assets.length }} 个媒体项
            </template>
          </p>
        </div>

        <div class="media-toolbar">
          <label class="select-all">
            <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll">
            <span>全选</span>
          </label>
          <button class="ghost-btn" type="button" :disabled="selectedIds.length === 0" @click="clearSelection">
            清空选择
          </button>
          <button class="remove-btn" type="button" :disabled="selectedIds.length === 0 || loading" @click="removeSelectedFavorites">
            <Trash2 :size="16" />
            移除
          </button>
        </div>
      </div>

      <div v-if="assets.length > 0" class="gallery-grid">
        <AssetCard
          v-for="asset in assets"
          :key="asset.id"
          :asset="asset"
          :selected="selectedIds.includes(asset.id)"
          :favorited="asset.favorited"
          :favorite-disabled="loading"
          :show-remove-action="true"
          :remove-disabled="loading"
          remove-title="从我的收藏移除"
          @preview="openViewer(asset.id)"
          @toggle-select="toggleSelection(asset.id)"
          @toggle-favorite="removeSingleFavorite(asset.id)"
          @remove="removeSingleFavorite(asset.id)"
        />
      </div>
      <div v-else class="empty-media-state">
        暂无收藏媒体项。
      </div>
    </section>

    <MediaViewer
      :open="viewerOpen"
      :assets="assets"
      :initial-index="viewerIndex"
      @close="viewerOpen = false"
    />

    <transition name="dialog-fade">
      <div v-if="messageDialog.visible" class="dialog-overlay" @click.self="closeMessageDialog">
        <div class="dialog-card message-dialog" :class="`message-${messageDialog.type}`">
          <h2>{{ messageDialog.title }}</h2>
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
import { Trash2 } from 'lucide-vue-next'
import AssetCard from '@/components/gallery/AssetCard.vue'
import MediaViewer from '@/components/gallery/MediaViewer.vue'
import { mediaApi } from '@/api/media'
import type { Asset } from '@/stores/media'

type MessageDialogType = 'info' | 'success' | 'error'

const assets = ref<Asset[]>([])
const selectedIds = ref<string[]>([])
const loading = ref(false)
const viewerOpen = ref(false)
const viewerIndex = ref(0)

const messageDialog = reactive({
  visible: false,
  title: '',
  message: '',
  type: 'info' as MessageDialogType,
})

const isAllSelected = computed(() => assets.value.length > 0 && selectedIds.value.length === assets.value.length)

const formatFileSize = (size?: number) => {
  if (!size) return '未知'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let value = size
  let unitIndex = 0
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024
    unitIndex += 1
  }
  return `${value.toFixed(value >= 10 ? 1 : 2)} ${units[unitIndex]}`
}

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

const adaptMediaAssets = (rawList: any[]): Asset[] =>
  rawList.map((item: any) => ({
    id: String(item.id),
    name: item.fileName,
    type: (item.fileFormat || 'JPG').toUpperCase(),
    mediaType: item.mediaType,
    date: (item.shotTime || item.createTime || '').split('T')[0],
    thumbColor: '#cbd5e1',
    url: item.thumbnailPath || '',
    filePath: item.filePath,
    fileSize: item.fileSize,
    favorited: Boolean(item.favorited),
    original: item,
    metadata: {
      camera: item.cameraModel ? `${item.cameraMake || ''} ${item.cameraModel}`.trim() : '设备信息未知',
      lens: item.lensModel || '未知',
      exposure: item.exposureTime ? `${item.exposureTime}s, f/${item.fnumber}, ISO ${item.iso}` : '无相机元数据',
      dimensions: item.width ? `${item.width} × ${item.height}` : '分辨率未知',
      shotTime: item.shotTime ? item.shotTime.replace('T', ' ') : '',
      format: (item.fileFormat || '').toUpperCase(),
      size: formatFileSize(item.fileSize),
      gps: item.gpsLatitude && item.gpsLongitude ? `${item.gpsLatitude}, ${item.gpsLongitude}` : '无数据',
      focalLength: item.focalLength ? `${item.focalLength}mm` : '无数据',
    },
  }))

const loadFavorites = async () => {
  loading.value = true
  try {
    const response: any = await mediaApi.fetchList({
      current: 1,
      size: 500,
      favoriteOnly: true,
    })
    assets.value = adaptMediaAssets(response.records || [])
    const validIds = new Set(assets.value.map(item => item.id))
    selectedIds.value = selectedIds.value.filter(id => validIds.has(id))
  } catch (error: any) {
    showMessageDialog('加载失败', extractErrorMessage(error, '收藏列表加载失败'), 'error')
  } finally {
    loading.value = false
  }
}

const openViewer = (assetId: string) => {
  const index = assets.value.findIndex(asset => asset.id === assetId)
  viewerIndex.value = index < 0 ? 0 : index
  viewerOpen.value = true
}

const toggleSelection = (assetId: string) => {
  selectedIds.value = selectedIds.value.includes(assetId)
    ? selectedIds.value.filter(id => id !== assetId)
    : [...selectedIds.value, assetId]
}

const toggleSelectAll = () => {
  selectedIds.value = isAllSelected.value ? [] : assets.value.map(asset => asset.id)
}

const clearSelection = () => {
  selectedIds.value = []
}

const removeSingleFavorite = async (assetId: string) => {
  if (loading.value) return

  loading.value = true
  try {
    viewerOpen.value = false
    await mediaApi.removeFavorite(assetId)
    assets.value = assets.value.filter(asset => asset.id !== assetId)
    selectedIds.value = selectedIds.value.filter(id => id !== assetId)
  } catch (error: any) {
    showMessageDialog('移除失败', extractErrorMessage(error, '移除收藏失败'), 'error')
  } finally {
    loading.value = false
  }
}

const removeSelectedFavorites = async () => {
  if (selectedIds.value.length === 0) return
  if (!confirm(`确认移除选中的 ${selectedIds.value.length} 个收藏吗？`)) return

  loading.value = true
  try {
    viewerOpen.value = false
    await Promise.all(selectedIds.value.map(id => mediaApi.removeFavorite(id)))
    const removedIds = new Set(selectedIds.value)
    assets.value = assets.value.filter(asset => !removedIds.has(asset.id))
    selectedIds.value = []
  } catch (error: any) {
    showMessageDialog('移除失败', extractErrorMessage(error, '批量移除收藏失败'), 'error')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadFavorites()
})
</script>

<style scoped>
.favorites-view {
  width: 100%;
}

.favorites-view.detail-mode {
  margin: -1.5rem -2.5rem 0;
}

.select-all,
.remove-btn,
.ghost-btn,
.primary-btn {
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 16px;
  border-radius: 14px;
  font-weight: 800;
  cursor: pointer;
}

.select-all,
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

.remove-btn {
  border: none;
  color: #fff;
  background: #dc2626;
}

.remove-btn:disabled,
.ghost-btn:disabled,
.primary-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.media-section {
  margin-top: 0;
  padding: 0;
}

.media-header {
  position: sticky;
  top: 0;
  z-index: 120;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
  padding: 0 2.5rem 14px;
  border-bottom: 1px solid var(--border);
  background: var(--bg);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.04);
}

.media-kicker {
  color: var(--text-muted);
  font-size: 0.8rem;
  font-weight: 800;
}

.media-title {
  margin-top: 6px;
  color: var(--text);
  font-size: 1.5rem;
  font-weight: 900;
}

.media-subtitle {
  margin-top: 8px;
  color: var(--text-muted);
  font-size: 0.92rem;
}

.media-toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 24px;
  padding: 0 2.5rem 2.5rem;
}

.empty-media-state {
  margin: 0 2.5rem 2.5rem;
  padding: 40px 20px;
  text-align: center;
  border-radius: 20px;
  color: var(--text-muted);
  background: var(--surface-hover);
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
  width: min(520px, 100%);
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

.dialog-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.message-dialog {
  width: min(420px, 100%);
}

.message-text {
  margin-top: 14px;
  color: var(--text-muted);
  line-height: 1.6;
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

@media (max-width: 900px) {
  .media-header {
    align-items: stretch;
    flex-direction: column;
  }

  .media-toolbar {
    justify-content: flex-start;
  }
}
</style>
