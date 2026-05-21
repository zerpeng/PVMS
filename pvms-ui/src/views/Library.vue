<template>
  <div class="library-view">
    <div class="view-header">
      <div>
        <h3 class="section-title">{{ headerTitle }}</h3>
        <p class="section-subtitle">{{ headerSubtitle }}</p>
      </div>

      <div v-if="!authInfo.visitor" class="view-actions">
        <button
          v-if="activeFilter"
          class="ghost-btn"
          type="button"
          @click="clearRouteFilter"
        >
          返回全部媒体
        </button>
        <label class="select-all">
          <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll">
          <span>全选</span>
        </label>
        <button class="filter-btn" type="button" :disabled="selectedIds.length === 0" @click="openAssignDialog('album')">
          <FolderPlus :size="16" /> 添加到相册
        </button>
        <button class="filter-btn" type="button" :disabled="selectedIds.length === 0" @click="openAssignDialog('tag')">
          <Tags :size="16" /> 添加到标签
        </button>
        <button class="ghost-btn" type="button" :disabled="selectedIds.length === 0" @click="clearSelection">
          清空选择
        </button>
        <button class="danger-btn" type="button" :disabled="selectedIds.length === 0 || loading" @click="removeSelectedMedia">
          <Trash2 :size="16" /> 移除
        </button>
      </div>
    </div>

    <div class="gallery-grid">
      <AssetCard
        v-for="asset in mediaStore.assets"
        :key="asset.id"
        :asset="asset"
        :selected="selectedIds.includes(asset.id)"
        :favorited="asset.favorited"
        :favorite-disabled="loading || authInfo.visitor"
        :show-remove-action="!authInfo.visitor"
        :remove-disabled="loading"
        remove-title="移除媒体"
        @preview="openViewer(asset.id)"
        @toggle-select="toggleSelection(asset.id)"
        @toggle-favorite="toggleFavorite(asset.id)"
        @remove="removeSingleMedia(asset.id)"
      />
    </div>

    <MediaViewer
      :open="viewerOpen"
      :assets="mediaStore.assets"
      :initial-index="viewerIndex"
      @close="viewerOpen = false"
      @assign-to-album="openSingleAssignDialog('album', $event)"
      @assign-to-tag="openSingleAssignDialog('tag', $event)"
    />

    <transition name="dialog-fade">
      <div v-if="assignDialog.visible" class="dialog-overlay" @click.self="closeAssignDialog">
        <div class="dialog-card">
          <h2>{{ assignDialog.type === 'album' ? '添加到相册' : '添加到标签' }}</h2>
          <p class="dialog-text">将 {{ assignDialog.mediaIds.length }} 个媒体项添加到{{ assignDialog.type === 'album' ? '相册' : '标签' }}。</p>

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
            <button class="ghost-btn" type="button" @click="closeAssignDialog">取消</button>
            <button
              class="primary-btn"
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
      <div v-if="messageDialog.visible" class="dialog-overlay" @click.self="closeMessageDialog">
        <div class="dialog-card message-dialog" :class="`message-${messageDialog.type}`">
          <h2>{{ messageDialog.title }}</h2>
          <p class="dialog-text">{{ messageDialog.message }}</p>
          <div class="dialog-actions">
            <button class="primary-btn" type="button" @click="closeMessageDialog">知道了</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { FolderPlus, Tags, Trash2 } from 'lucide-vue-next'
import AssetCard from '@/components/gallery/AssetCard.vue'
import MediaViewer from '@/components/gallery/MediaViewer.vue'
import { useMediaStore } from '@/stores/media'
import { mediaApi } from '@/api/media'
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
const route = useRoute()
const router = useRouter()
const authInfo = computed(() => getAuthInfo())
const isLoading = ref(false)
const loading = ref(false)
const viewerOpen = ref(false)
const viewerIndex = ref(0)
const selectedIds = ref<string[]>([])
const albums = ref<AlbumItem[]>([])
const tags = ref<TagItem[]>([])

const assignDialog = reactive({
  visible: false,
  type: 'album' as AssignType,
  mediaIds: [] as string[],
  targetId: null as number | null,
})

const messageDialog = reactive({
  visible: false,
  title: '',
  message: '',
  type: 'info' as MessageType,
})

const isAllSelected = computed(() => mediaStore.assets.length > 0 && selectedIds.value.length === mediaStore.assets.length)
const activeFilter = computed(() => {
  const albumId = route.query.albumId
  const tagId = route.query.tagId
  return albumId || tagId
})

const headerTitle = computed(() => {
  if (route.query.albumId) {
    return `相册：${String(route.query.albumName || route.query.albumId)}`
  }
  if (route.query.tagId) {
    return `标签：${String(route.query.tagName || route.query.tagId)}`
  }
  return '最近上传的媒体'
})

const headerSubtitle = computed(() => {
  if (selectedIds.value.length > 0) {
    return `已选中 ${selectedIds.value.length} 项`
  }
  if (route.query.albumId) {
    return '当前展示该相册下已绑定的图片和视频'
  }
  if (route.query.tagId) {
    return '当前展示该标签下已绑定的图片和视频'
  }
  return '支持图片与视频批量添加到相册或标签'
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

const showMessageDialog = (title: string, message: string, type: MessageType = 'info') => {
  messageDialog.title = title
  messageDialog.message = message
  messageDialog.type = type
  messageDialog.visible = true
}

const closeMessageDialog = () => {
  messageDialog.visible = false
}

const openViewer = (assetId: string) => {
  const index = mediaStore.assets.findIndex(asset => asset.id === assetId)
  viewerIndex.value = index < 0 ? 0 : index
  viewerOpen.value = true
}

const loadReferenceData = async () => {
  const [albumData, tagData] = await Promise.all([
    mediaApi.fetchAlbums(),
    mediaApi.fetchTags(),
  ])
  albums.value = Array.isArray(albumData) ? albumData : []
  tags.value = Array.isArray(tagData) ? tagData : []
}

const loadMediaData = async () => {
  isLoading.value = true
  try {
    const response: any = await mediaApi.fetchList({
      albumId: route.query.albumId ? Number(route.query.albumId) : undefined,
      tagId: route.query.tagId ? Number(route.query.tagId) : undefined,
    })
    const rawList = response.records || []

    const adaptedData = rawList.map((item: any) => ({
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
        camera: item.cameraModel ? `${item.cameraMake || ''} ${item.cameraModel}` : '设备信息未知',
        lens: item.lensModel || '未知',
        exposure: item.exposureTime ? `${item.exposureTime}s, f/${item.fnumber}, ISO ${item.iso}` : '无相机元数据',
        dimensions: item.width ? `${item.width} × ${item.height}` : '分辨率未知',
        shotTime: item.shotTime ? item.shotTime.replace('T', ' ') : '',
        format: (item.fileFormat || '').toUpperCase(),
        size: formatFileSize(item.fileSize),
        gps: item.gpsLatitude && item.gpsLongitude ? `${item.gpsLatitude}, ${item.gpsLongitude}` : '无数据',
        focalLength: item.focalLength ? `${item.focalLength}mm` : '无数据'
      }
    }))

    mediaStore.setAssets(adaptedData)
    const validIds = new Set(adaptedData.map((item: { id: string }) => item.id))
    selectedIds.value = selectedIds.value.filter(id => validIds.has(id))
  } catch (error: any) {
    showMessageDialog('加载失败', extractErrorMessage(error, '媒体列表加载失败'), 'error')
  } finally {
    isLoading.value = false
  }
}

const toggleSelection = (assetId: string) => {
  selectedIds.value = selectedIds.value.includes(assetId)
    ? selectedIds.value.filter(id => id !== assetId)
    : [...selectedIds.value, assetId]
}

const updateAssetFavoriteState = (assetId: string, favorited: boolean) => {
  mediaStore.setAssets(
    mediaStore.assets.map(asset => asset.id === assetId ? { ...asset, favorited } : asset)
  )
}

const toggleFavorite = async (assetId: string) => {
  const asset = mediaStore.assets.find(item => item.id === assetId)
  if (!asset || loading.value) return

  loading.value = true
  try {
    if (asset.favorited) {
      await mediaApi.removeFavorite(assetId)
      updateAssetFavoriteState(assetId, false)
    } else {
      await mediaApi.addFavorite(assetId)
      updateAssetFavoriteState(assetId, true)
    }
  } catch (error: any) {
    showMessageDialog('收藏失败', extractErrorMessage(error, '更新收藏状态失败'), 'error')
  } finally {
    loading.value = false
  }
}

const removeMediaByIds = async (ids: string[]) => {
  await Promise.all(ids.map(id => mediaApi.removeMediaFile(id)))
  viewerOpen.value = false
  selectedIds.value = selectedIds.value.filter(id => !ids.includes(id))
  mediaStore.setAssets(mediaStore.assets.filter(asset => !ids.includes(asset.id)))
}

const removeSingleMedia = async (assetId: string) => {
  if (!confirm('确认移除该媒体吗？')) return

  loading.value = true
  try {
    await removeMediaByIds([assetId])
    showMessageDialog('移除成功', '媒体已移除', 'success')
  } catch (error: any) {
    showMessageDialog('移除失败', extractErrorMessage(error, '移除媒体失败'), 'error')
  } finally {
    loading.value = false
  }
}

const removeSelectedMedia = async () => {
  if (selectedIds.value.length === 0) return
  if (!confirm(`确认移除选中的 ${selectedIds.value.length} 个媒体吗？`)) return

  loading.value = true
  try {
    await removeMediaByIds([...selectedIds.value])
    showMessageDialog('移除成功', '选中的媒体已移除', 'success')
  } catch (error: any) {
    showMessageDialog('移除失败', extractErrorMessage(error, '批量移除媒体失败'), 'error')
  } finally {
    loading.value = false
  }
}

const toggleSelectAll = () => {
  selectedIds.value = isAllSelected.value ? [] : mediaStore.assets.map(asset => asset.id)
}

const clearSelection = () => {
  selectedIds.value = []
}

const clearRouteFilter = () => {
  router.push({ name: 'library' })
}

const openAssignDialog = async (type: AssignType) => {
  if (selectedIds.value.length === 0) return
  await loadReferenceData()
  assignDialog.type = type
  assignDialog.mediaIds = [...selectedIds.value]
  assignDialog.targetId = null
  assignDialog.visible = true
}

const openSingleAssignDialog = async (type: AssignType, mediaId: string) => {
  await loadReferenceData()
  assignDialog.type = type
  assignDialog.mediaIds = [mediaId]
  assignDialog.targetId = null
  assignDialog.visible = true
}

const closeAssignDialog = () => {
  assignDialog.visible = false
}

const submitAssign = async () => {
  if (!assignDialog.targetId || assignDialog.mediaIds.length === 0) return

  loading.value = true
  try {
    if (assignDialog.type === 'album') {
      await Promise.all(
        assignDialog.mediaIds.map(mediaId => mediaApi.addMediaToAlbum(assignDialog.targetId as number, mediaId))
      )
    } else {
      await Promise.all(
        assignDialog.mediaIds.map(mediaId => mediaApi.bindTagToMedia(mediaId, assignDialog.targetId as number))
      )
    }

    const targetLabel = assignDialog.type === 'album' ? '相册' : '标签'
    closeAssignDialog()
    showMessageDialog('添加成功', `已将 ${assignDialog.mediaIds.length} 个媒体项添加到${targetLabel}`, 'success')
  } catch (error: any) {
    showMessageDialog('添加失败', extractErrorMessage(error, '添加媒体关系失败'), 'error')
  } finally {
    loading.value = false
  }
}

watch(
  () => [route.query.albumId, route.query.tagId],
  async () => {
    clearSelection()
    await loadMediaData()
  }
)

onMounted(async () => {
  await Promise.all([loadMediaData(), loadReferenceData()])
})
</script>

<style scoped>
.library-view { width: 100%; }
.view-header { margin-bottom: 2rem; display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; }
.section-title { font-size: 1.1rem; color: var(--text-muted); }
.section-subtitle { margin-top: 0.35rem; font-size: 0.85rem; color: var(--text-muted); }
.view-actions { display: flex; gap: 0.75rem; flex-wrap: wrap; justify-content: flex-end; }
.filter-btn, .ghost-btn, .select-all, .danger-btn { display: flex; align-items: center; gap: 8px; padding: 0 1rem; height: 44px; border-radius: 12px; background: var(--surface); border: 1px solid var(--border); color: var(--text); font-size: 0.85rem; cursor: pointer; transition: all 0.2s; }
.danger-btn { border: none; color: #fff; background: #dc2626; }
.filter-btn:hover, .ghost-btn:hover, .danger-btn:hover { background: var(--surface-hover); transform: translateY(-2px); }
.danger-btn:hover { background: #b91c1c; }
.filter-btn:disabled, .ghost-btn:disabled, .danger-btn:disabled { opacity: 0.45; cursor: not-allowed; transform: none; }
.gallery-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 2.5rem; }
.dialog-overlay { position: fixed; inset: 0; z-index: 360; display: flex; align-items: center; justify-content: center; padding: 24px; background: rgba(15, 23, 42, 0.22); backdrop-filter: blur(4px); }
.dialog-card { width: min(520px, 100%); padding: 28px; border-radius: 24px; background: var(--surface); border: 1px solid var(--border); box-shadow: 0 24px 80px rgba(15, 23, 42, 0.24); }
.dialog-card h2 { color: var(--text); font-size: 1.25rem; }
.dialog-text { margin-top: 10px; color: var(--text-muted); line-height: 1.6; }
.target-list { margin-top: 20px; display: flex; flex-direction: column; gap: 12px; max-height: 320px; overflow-y: auto; }
.target-item { display: flex; gap: 12px; padding: 14px 16px; border: 1px solid var(--border); border-radius: 16px; background: var(--surface-hover); cursor: pointer; }
.target-item.active { border-color: var(--accent); box-shadow: 0 12px 28px rgba(37, 99, 235, 0.12); }
.target-item input { margin-top: 4px; }
.target-item strong { color: var(--text); display: block; }
.target-item p { margin-top: 6px; color: var(--text-muted); font-size: 0.82rem; }
.empty-state { margin-top: 20px; padding: 24px; border-radius: 16px; background: var(--surface-hover); color: var(--text-muted); line-height: 1.6; }
.dialog-actions { margin-top: 24px; display: flex; justify-content: flex-end; gap: 12px; }
.primary-btn { height: 42px; padding: 0 18px; border-radius: 14px; font-weight: 800; cursor: pointer; border: none; color: #fff; background: var(--accent); }
.primary-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.message-dialog { width: min(420px, 100%); }
.message-success { border-color: rgba(22, 163, 74, 0.22); }
.message-error { border-color: rgba(220, 38, 38, 0.22); }
.message-info { border-color: rgba(37, 99, 235, 0.22); }
.dialog-fade-enter-active, .dialog-fade-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.dialog-fade-enter-from, .dialog-fade-leave-to { opacity: 0; transform: scale(0.98); }
@media (max-width: 900px) {
  .view-header { flex-direction: column; align-items: stretch; }
  .view-actions { justify-content: flex-start; }
}
</style>
