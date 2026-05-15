<template>
  <div :class="['album-tags-view', { 'detail-mode': hasActiveMediaView }]">
    <header v-if="!hasActiveMediaView" class="page-header">
      <div>
        <p class="eyebrow">组织管理</p>
        <h1 class="page-title">相册标签</h1>
      </div>

      <div v-if="!hasActiveMediaView" class="header-actions">
        <div class="tabs">
          <button
            type="button"
            :class="['tab-btn', { active: activeTab === 'albums' }]"
            @click="switchTab('albums')"
          >
            相册
          </button>
          <button
            type="button"
            :class="['tab-btn', { active: activeTab === 'tags' }]"
            @click="switchTab('tags')"
          >
            标签
          </button>
        </div>

        <label class="select-all">
          <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll">
          <span>全选</span>
        </label>
        <button class="add-btn" type="button" :disabled="loading" @click="openCreateDialog">
          <Plus :size="16" />
          新增{{ activeTab === 'albums' ? '相册' : '标签' }}
        </button>
        <button
          class="remove-btn"
          type="button"
          :disabled="currentSelectedIds.length === 0 || loading"
          @click="removeSelected"
        >
          <Trash2 :size="16" />
          移除选中
        </button>
      </div>
    </header>

    <section v-if="!hasActiveMediaView && activeTab === 'albums'" class="card-grid">
      <button class="add-card" type="button" @click="openCreateDialog">
        <div class="add-icon">
          <Plus :size="42" />
        </div>
        <span>新增相册</span>
      </button>

      <article
        v-for="album in albums"
        :key="album.id"
        :class="[
          'entity-card',
          {
            selected: selectedAlbumIds.includes(album.id),
            active: mediaView.type === 'albums' && mediaView.id === album.id,
          },
        ]"
        @click="openAlbumMediaList(album)"
      >
        <input
          class="entity-check"
          type="checkbox"
          :checked="selectedAlbumIds.includes(album.id)"
          @click.stop
          @change="toggleAlbumSelection(album.id)"
        >
        <div class="entity-top">
          <div class="entity-icon album">
            <FolderKanban :size="28" />
          </div>
          <span class="entity-badge">{{ album.albumType === 2 ? '智能相册' : '普通相册' }}</span>
        </div>
        <h3>{{ album.name }}</h3>
        <p class="entity-desc">{{ album.description || '暂无相册描述' }}</p>
        <div class="entity-meta">
          <span>创建时间 {{ formatTime(album.createTime) }}</span>
          <span v-if="album.albumType === 2">{{ album.queryRule ? '已配置规则' : '未配置规则' }}</span>
        </div>
        <div class="card-actions" @click.stop>
          <button class="ghost-btn" type="button" @click="openEditAlbumDialog(album)">编辑</button>
          <button class="danger-text-btn" type="button" @click="removeAlbum(album.id)">移除</button>
        </div>
      </article>
    </section>

    <section v-else-if="!hasActiveMediaView" class="card-grid">
      <button class="add-card" type="button" @click="openCreateDialog">
        <div class="add-icon">
          <Plus :size="42" />
        </div>
        <span>新增标签</span>
      </button>

      <article
        v-for="tag in tags"
        :key="tag.id"
        :class="[
          'entity-card',
          {
            selected: selectedTagIds.includes(tag.id),
            active: mediaView.type === 'tags' && mediaView.id === tag.id,
          },
        ]"
        @click="openTagMediaList(tag)"
      >
        <input
          class="entity-check"
          type="checkbox"
          :checked="selectedTagIds.includes(tag.id)"
          @click.stop
          @change="toggleTagSelection(tag.id)"
        >
        <div class="entity-top">
          <div class="entity-icon tag">
            <Tags :size="28" />
          </div>
          <span class="entity-badge">{{ tag.tagType === 2 ? 'AI 标签' : '手动标签' }}</span>
        </div>
        <h3>{{ tag.name }}</h3>
        <p class="entity-desc">{{ tag.category || '未设置分类' }}</p>
        <div class="entity-meta">
          <span>{{ tag.parentId ? `父标签 ${getParentTagName(tag.parentId)}` : '顶级标签' }}</span>
          <span>ID {{ tag.id }}</span>
        </div>
        <div class="card-actions" @click.stop>
          <button class="ghost-btn" type="button" @click="openEditTagDialog(tag)">编辑</button>
          <button class="danger-text-btn" type="button" @click="removeTag(tag.id)">移除</button>
        </div>
      </article>
    </section>

    <section v-if="hasActiveMediaView" class="media-section">
      <div class="media-header">
        <div>
          <p class="media-kicker">当前{{ activeTab === 'albums' ? '相册' : '标签' }}</p>
          <h2 class="media-title">{{ mediaView.name }}</h2>
          <div class="media-subtitle-row">
            <p class="media-subtitle">
              <template v-if="selectedMediaIds.length > 0">
                已选中 {{ selectedMediaIds.length }} 项
              </template>
              <template v-else>
                共 {{ mediaAssets.length }} 个媒体项
              </template>
            </p>
            <button class="back-btn" type="button" @click="backToManagePage">
              返回
            </button>
          </div>
        </div>

        <div class="media-toolbar">
          <label class="select-all">
            <input type="checkbox" :checked="isAllMediaSelected" @change="toggleSelectAllMedia">
            <span>全选</span>
          </label>
          <button class="ghost-btn" type="button" :disabled="loading" @click="removeCurrentCollection">
            移除{{ activeTab === 'albums' ? '相册' : '标签' }}
          </button>
          <button class="ghost-btn" type="button" :disabled="selectedMediaIds.length === 0" @click="clearMediaSelection">
            清空选择
          </button>
          <button class="remove-btn" type="button" :disabled="selectedMediaIds.length === 0 || loading" @click="removeSelectedMedia">
            <Trash2 :size="16" />
            移除
          </button>
        </div>
      </div>

      <div v-if="mediaAssets.length > 0" class="gallery-grid">
        <AssetCard
          v-for="asset in mediaAssets"
          :key="asset.id"
          :asset="asset"
          :selected="selectedMediaIds.includes(asset.id)"
          :show-remove-action="true"
          :remove-disabled="loading"
          :remove-title="activeTab === 'albums' ? '从当前相册移除' : '从当前标签移除'"
          @preview="openViewer(asset.id)"
          @toggle-select="toggleMediaSelection(asset.id)"
          @remove="removeSingleMedia(asset.id)"
        />
      </div>
      <div v-else class="empty-media-state">
        当前{{ activeTab === 'albums' ? '相册' : '标签' }}下暂无媒体项。
      </div>
    </section>

    <MediaViewer
      :open="viewerOpen"
      :assets="mediaAssets"
      :initial-index="viewerIndex"
      @close="viewerOpen = false"
    />

    <transition name="dialog-fade">
      <div v-if="dialogOpen" class="dialog-overlay" @click.self="dialogOpen = false">
        <div class="dialog-card">
          <h2>{{ dialogMode === 'create' ? `新增${activeTab === 'albums' ? '相册' : '标签'}` : `编辑${activeTab === 'albums' ? '相册' : '标签'}` }}</h2>

          <template v-if="activeTab === 'albums'">
            <label class="field">
              <span>相册名称</span>
              <input v-model.trim="albumForm.name" type="text" placeholder="请输入相册名称">
            </label>
            <label class="field">
              <span>相册类型</span>
              <select v-model.number="albumForm.albumType">
                <option :value="1">普通相册</option>
                <option :value="2">智能相册</option>
              </select>
            </label>
            <label class="field">
              <span>描述</span>
              <textarea v-model.trim="albumForm.description" rows="4" placeholder="可填写相册描述"></textarea>
            </label>
            <label v-if="albumForm.albumType === 2" class="field">
              <span>规则</span>
              <textarea v-model.trim="albumForm.queryRule" rows="4" placeholder="可填写智能相册筛选规则"></textarea>
            </label>
          </template>

          <template v-else>
            <label class="field">
              <span>标签名称</span>
              <input v-model.trim="tagForm.name" type="text" placeholder="请输入标签名称">
            </label>
            <label class="field">
              <span>标签类型</span>
              <select v-model.number="tagForm.tagType">
                <option :value="1">手动标签</option>
                <option :value="2">AI 标签</option>
              </select>
            </label>
            <label class="field">
              <span>标签分类</span>
              <input v-model.trim="tagForm.category" type="text" placeholder="例如：人物、地点、风景">
            </label>
            <label class="field">
              <span>父标签</span>
              <select v-model.number="tagForm.parentId">
                <option :value="0">无父标签</option>
                <option
                  v-for="option in parentTagOptions"
                  :key="option.id"
                  :value="option.id"
                >
                  {{ option.name }}
                </option>
              </select>
            </label>
          </template>

          <div class="dialog-actions">
            <button class="ghost-btn" type="button" @click="dialogOpen = false">取消</button>
            <button class="primary-btn" type="button" :disabled="loading" @click="submitDialog">
              {{ dialogMode === 'create' ? '创建' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { FolderKanban, Plus, Tags, Trash2 } from 'lucide-vue-next'
import AssetCard from '@/components/gallery/AssetCard.vue'
import MediaViewer from '@/components/gallery/MediaViewer.vue'
import { mediaApi } from '@/api/media'
import type { Asset } from '@/stores/media'

type TabKey = 'albums' | 'tags'
type DialogMode = 'create' | 'edit'
type MessageDialogType = 'info' | 'success' | 'error'

interface AlbumItem {
  id: number
  name: string
  description?: string | null
  albumType?: number
  queryRule?: string | null
  createTime?: string
}

interface TagItem {
  id: number
  name: string
  parentId?: number
  tagType?: number
  category?: string | null
}

interface MediaViewState {
  type: TabKey | null
  id: number | null
  name: string
}

const activeTab = ref<TabKey>('albums')
const dialogOpen = ref(false)
const dialogMode = ref<DialogMode>('create')
const loading = ref(false)
const viewerOpen = ref(false)
const viewerIndex = ref(0)

const albums = ref<AlbumItem[]>([])
const tags = ref<TagItem[]>([])
const mediaAssets = ref<Asset[]>([])
const selectedAlbumIds = ref<number[]>([])
const selectedTagIds = ref<number[]>([])
const selectedMediaIds = ref<string[]>([])
const editingAlbumId = ref<number | null>(null)
const editingTagId = ref<number | null>(null)

const mediaView = reactive<MediaViewState>({
  type: null,
  id: null,
  name: '',
})

const albumForm = reactive({
  name: '',
  description: '',
  albumType: 1,
  queryRule: '',
})

const tagForm = reactive({
  name: '',
  tagType: 1,
  category: '',
  parentId: 0,
})

const messageDialog = reactive({
  visible: false,
  title: '',
  message: '',
  type: 'info' as MessageDialogType,
})

const currentSelectedIds = computed(() =>
  activeTab.value === 'albums' ? selectedAlbumIds.value : selectedTagIds.value
)

const isAllSelected = computed(() => {
  const items = activeTab.value === 'albums' ? albums.value : tags.value
  return items.length > 0 && currentSelectedIds.value.length === items.length
})

const isAllMediaSelected = computed(() =>
  mediaAssets.value.length > 0 && selectedMediaIds.value.length === mediaAssets.value.length
)

const hasActiveMediaView = computed(() =>
  mediaView.type === activeTab.value && mediaView.id != null
)

const parentTagOptions = computed(() =>
  tags.value.filter(tag => tag.id !== editingTagId.value)
)

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

const formatTime = (value?: string) => {
  if (!value) return '未知'
  return value.replace('T', ' ').slice(0, 16)
}

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

const getParentTagName = (parentId?: number) => {
  if (!parentId) return '无父标签'
  return tags.value.find(tag => tag.id === parentId)?.name || `#${parentId}`
}

const resetAlbumForm = () => {
  albumForm.name = ''
  albumForm.description = ''
  albumForm.albumType = 1
  albumForm.queryRule = ''
  editingAlbumId.value = null
}

const resetTagForm = () => {
  tagForm.name = ''
  tagForm.tagType = 1
  tagForm.category = ''
  tagForm.parentId = 0
  editingTagId.value = null
}

const clearMediaView = () => {
  viewerOpen.value = false
  viewerIndex.value = 0
  mediaAssets.value = []
  selectedMediaIds.value = []
  mediaView.type = null
  mediaView.id = null
  mediaView.name = ''
}

const syncMediaViewState = () => {
  if (!hasActiveMediaView.value) return

  const source = activeTab.value === 'albums' ? albums.value : tags.value
  const current = source.find(item => item.id === mediaView.id)

  if (!current) {
    clearMediaView()
    return
  }

  mediaView.name = current.name
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

const loadAlbums = async () => {
  const data = await mediaApi.fetchAlbums()
  albums.value = Array.isArray(data) ? data : []
  const validIds = new Set(albums.value.map(item => item.id))
  selectedAlbumIds.value = selectedAlbumIds.value.filter(id => validIds.has(id))
  syncMediaViewState()
}

const loadTags = async () => {
  const data = await mediaApi.fetchTags()
  tags.value = Array.isArray(data) ? data : []
  const validIds = new Set(tags.value.map(item => item.id))
  selectedTagIds.value = selectedTagIds.value.filter(id => validIds.has(id))
  syncMediaViewState()
}

const loadMediaData = async () => {
  if (!hasActiveMediaView.value || mediaView.id == null) {
    mediaAssets.value = []
    selectedMediaIds.value = []
    return
  }

  const response: any = await mediaApi.fetchList({
    current: 1,
    size: 500,
    albumId: mediaView.type === 'albums' ? mediaView.id : undefined,
    tagId: mediaView.type === 'tags' ? mediaView.id : undefined,
  })

  const assets = adaptMediaAssets(response.records || [])
  mediaAssets.value = assets
  const validIds = new Set(assets.map(item => item.id))
  selectedMediaIds.value = selectedMediaIds.value.filter(id => validIds.has(id))
}

const loadPageData = async () => {
  loading.value = true
  try {
    await Promise.all([loadAlbums(), loadTags()])
    await loadMediaData()
  } catch (error: any) {
    showMessageDialog('加载失败', extractErrorMessage(error, '相册标签数据加载失败'), 'error')
  } finally {
    loading.value = false
  }
}

const toggleAlbumSelection = (id: number) => {
  selectedAlbumIds.value = selectedAlbumIds.value.includes(id)
    ? selectedAlbumIds.value.filter(item => item !== id)
    : [...selectedAlbumIds.value, id]
}

const toggleTagSelection = (id: number) => {
  selectedTagIds.value = selectedTagIds.value.includes(id)
    ? selectedTagIds.value.filter(item => item !== id)
    : [...selectedTagIds.value, id]
}

const toggleMediaSelection = (id: string) => {
  selectedMediaIds.value = selectedMediaIds.value.includes(id)
    ? selectedMediaIds.value.filter(item => item !== id)
    : [...selectedMediaIds.value, id]
}

const switchTab = (tab: TabKey) => {
  if (activeTab.value === tab) return
  activeTab.value = tab
}

const openAlbumMediaList = async (album: AlbumItem) => {
  mediaView.type = 'albums'
  mediaView.id = album.id
  mediaView.name = album.name
  selectedMediaIds.value = []
  viewerOpen.value = false
  loading.value = true
  try {
    await loadMediaData()
  } catch (error: any) {
    showMessageDialog('加载失败', extractErrorMessage(error, '相册媒体列表加载失败'), 'error')
  } finally {
    loading.value = false
  }
}

const openTagMediaList = async (tag: TagItem) => {
  mediaView.type = 'tags'
  mediaView.id = tag.id
  mediaView.name = tag.name
  selectedMediaIds.value = []
  viewerOpen.value = false
  loading.value = true
  try {
    await loadMediaData()
  } catch (error: any) {
    showMessageDialog('加载失败', extractErrorMessage(error, '标签媒体列表加载失败'), 'error')
  } finally {
    loading.value = false
  }
}

const openViewer = (assetId: string) => {
  const index = mediaAssets.value.findIndex(asset => asset.id === assetId)
  viewerIndex.value = index < 0 ? 0 : index
  viewerOpen.value = true
}

const toggleSelectAll = () => {
  if (activeTab.value === 'albums') {
    selectedAlbumIds.value = isAllSelected.value ? [] : albums.value.map(item => item.id)
  } else {
    selectedTagIds.value = isAllSelected.value ? [] : tags.value.map(item => item.id)
  }
}

const toggleSelectAllMedia = () => {
  selectedMediaIds.value = isAllMediaSelected.value ? [] : mediaAssets.value.map(item => item.id)
}

const clearMediaSelection = () => {
  selectedMediaIds.value = []
}

const backToManagePage = () => {
  clearMediaView()
}

const openCreateDialog = () => {
  dialogMode.value = 'create'
  if (activeTab.value === 'albums') {
    resetAlbumForm()
  } else {
    resetTagForm()
  }
  dialogOpen.value = true
}

const openEditAlbumDialog = (album: AlbumItem) => {
  dialogMode.value = 'edit'
  editingAlbumId.value = album.id
  albumForm.name = album.name
  albumForm.description = album.description || ''
  albumForm.albumType = album.albumType || 1
  albumForm.queryRule = album.queryRule || ''
  dialogOpen.value = true
}

const openEditTagDialog = (tag: TagItem) => {
  dialogMode.value = 'edit'
  editingTagId.value = tag.id
  tagForm.name = tag.name
  tagForm.tagType = tag.tagType || 1
  tagForm.category = tag.category || ''
  tagForm.parentId = tag.parentId || 0
  dialogOpen.value = true
}

const submitDialog = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'albums') {
      if (!albumForm.name.trim()) {
        showMessageDialog('保存失败', '请输入相册名称', 'error')
        return
      }

      const payload = {
        name: albumForm.name,
        description: albumForm.description || null,
        albumType: albumForm.albumType,
        queryRule: albumForm.albumType === 2 ? (albumForm.queryRule || null) : null,
      }

      if (dialogMode.value === 'create') {
        await mediaApi.createAlbum(payload)
      } else if (editingAlbumId.value != null) {
        await mediaApi.updateAlbum(editingAlbumId.value, payload)
      }

      await loadAlbums()
      dialogOpen.value = false
      showMessageDialog('保存成功', `相册已${dialogMode.value === 'create' ? '创建' : '更新'}`, 'success')
    } else {
      if (!tagForm.name.trim()) {
        showMessageDialog('保存失败', '请输入标签名称', 'error')
        return
      }

      const payload = {
        name: tagForm.name,
        tagType: tagForm.tagType,
        category: tagForm.category || null,
        parentId: tagForm.parentId,
      }

      if (dialogMode.value === 'create') {
        await mediaApi.createTag(payload)
      } else if (editingTagId.value != null) {
        await mediaApi.updateTag(editingTagId.value, payload)
      }

      await loadTags()
      dialogOpen.value = false
      showMessageDialog('保存成功', `标签已${dialogMode.value === 'create' ? '创建' : '更新'}`, 'success')
    }

    if (hasActiveMediaView.value) {
      await loadMediaData()
    }
  } catch (error: any) {
    showMessageDialog('保存失败', extractErrorMessage(error, '保存数据失败'), 'error')
  } finally {
    loading.value = false
  }
}

const removeAlbum = async (id: number) => {
  if (!confirm('确认移除该相册吗？')) return

  loading.value = true
  try {
    await mediaApi.removeAlbum(id)
    if (mediaView.type === 'albums' && mediaView.id === id) {
      clearMediaView()
    }
    await loadAlbums()
    showMessageDialog('移除成功', '相册已移除', 'success')
  } catch (error: any) {
    showMessageDialog('移除失败', extractErrorMessage(error, '移除相册失败'), 'error')
  } finally {
    loading.value = false
  }
}

const removeTag = async (id: number) => {
  if (!confirm('确认移除该标签吗？')) return

  loading.value = true
  try {
    await mediaApi.removeTag(id)
    if (mediaView.type === 'tags' && mediaView.id === id) {
      clearMediaView()
    }
    await loadTags()
    showMessageDialog('移除成功', '标签已移除', 'success')
  } catch (error: any) {
    showMessageDialog('移除失败', extractErrorMessage(error, '移除标签失败'), 'error')
  } finally {
    loading.value = false
  }
}

const removeSelected = async () => {
  if (currentSelectedIds.value.length === 0) return

  const label = activeTab.value === 'albums' ? '相册' : '标签'
  if (!confirm(`确认移除选中的 ${currentSelectedIds.value.length} 个${label}吗？`)) return

  loading.value = true
  try {
    if (activeTab.value === 'albums') {
      await mediaApi.removeAlbums(selectedAlbumIds.value)
      if (mediaView.type === 'albums' && mediaView.id != null && selectedAlbumIds.value.includes(mediaView.id)) {
        clearMediaView()
      }
      selectedAlbumIds.value = []
      await loadAlbums()
    } else {
      await mediaApi.removeTags(selectedTagIds.value)
      if (mediaView.type === 'tags' && mediaView.id != null && selectedTagIds.value.includes(mediaView.id)) {
        clearMediaView()
      }
      selectedTagIds.value = []
      await loadTags()
    }
    showMessageDialog('移除成功', `选中的${label}已移除`, 'success')
  } catch (error: any) {
    showMessageDialog('移除失败', extractErrorMessage(error, `批量移除${label}失败`), 'error')
  } finally {
    loading.value = false
  }
}

const removeCurrentCollection = async () => {
  if (!hasActiveMediaView.value || mediaView.id == null) return
  if (mediaView.type === 'albums') {
    await removeAlbum(mediaView.id)
  } else {
    await removeTag(mediaView.id)
  }
}

const removeRelation = async (mediaId: string) => {
  if (!hasActiveMediaView.value || mediaView.id == null) return

  if (mediaView.type === 'albums') {
    await mediaApi.removeMediaFromAlbum(mediaView.id, mediaId)
  } else {
    await mediaApi.removeMediaFromTag(mediaView.id, mediaId)
  }
}

const removeSingleMedia = async (mediaId: string) => {
  if (!confirm(`确认${activeTab.value === 'albums' ? '从当前相册' : '从当前标签'}移除该媒体项吗？`)) return

  loading.value = true
  try {
    viewerOpen.value = false
    await removeRelation(mediaId)
    await loadMediaData()
    showMessageDialog('移除成功', '媒体项已移除', 'success')
  } catch (error: any) {
    showMessageDialog('移除失败', extractErrorMessage(error, '移除媒体项失败'), 'error')
  } finally {
    loading.value = false
  }
}

const removeSelectedMedia = async () => {
  if (selectedMediaIds.value.length === 0) return

  const relationLabel = activeTab.value === 'albums' ? '相册' : '标签'
  if (!confirm(`确认从当前${relationLabel}移除选中的 ${selectedMediaIds.value.length} 个媒体项吗？`)) return

  loading.value = true
  try {
    viewerOpen.value = false
    await Promise.all(selectedMediaIds.value.map(mediaId => removeRelation(mediaId)))
    selectedMediaIds.value = []
    await loadMediaData()
    showMessageDialog('移除成功', '选中的媒体项已移除', 'success')
  } catch (error: any) {
    showMessageDialog('移除失败', extractErrorMessage(error, '批量移除媒体项失败'), 'error')
  } finally {
    loading.value = false
  }
}

watch(activeTab, () => {
  clearMediaView()
})

onMounted(() => {
  loadPageData()
})
</script>

<style scoped>
.album-tags-view {
  width: 100%;
}

.album-tags-view.detail-mode {
  margin: -1.5rem -2.5rem 0;
}

.page-header {
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
  flex-wrap: wrap;
  justify-content: flex-end;
}

.tabs {
  display: inline-flex;
  padding: 4px;
  border-radius: 16px;
  border: 1px solid var(--border);
  background: var(--surface);
}

.tab-btn {
  height: 40px;
  min-width: 88px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: var(--text-muted);
  font-weight: 800;
  cursor: pointer;
}

.tab-btn.active {
  background: var(--accent);
  color: #fff;
}

.select-all,
.add-btn,
.remove-btn,
.back-btn,
.ghost-btn,
.primary-btn,
.danger-text-btn {
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 16px;
  border-radius: 14px;
  font-weight: 800;
}

.select-all,
.back-btn,
.ghost-btn {
  border: 1px solid var(--border);
  color: var(--text);
  background: var(--surface);
}

.add-btn,
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

.danger-text-btn {
  border: none;
  color: #dc2626;
  background: rgba(220, 38, 38, 0.08);
}

.tab-btn,
.select-all,
.add-btn,
.remove-btn,
.back-btn,
.ghost-btn,
.primary-btn,
.danger-text-btn {
  cursor: pointer;
}

.add-btn:disabled,
.remove-btn:disabled,
.back-btn:disabled,
.ghost-btn:disabled,
.primary-btn:disabled,
.danger-text-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 24px;
}

.add-card,
.entity-card {
  min-height: 260px;
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

.entity-card {
  position: relative;
  padding: 24px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.entity-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--card-shadow-hover);
}

.entity-card.selected,
.entity-card.active {
  border-color: var(--accent);
  box-shadow: 0 18px 40px rgba(37, 99, 235, 0.18);
}

.entity-check {
  position: absolute;
  top: 18px;
  right: 18px;
  width: 18px;
  height: 18px;
}

.entity-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.entity-icon {
  width: 58px;
  height: 58px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.entity-icon.album {
  background: rgba(14, 165, 233, 0.12);
  color: #0284c7;
}

.entity-icon.tag {
  background: rgba(245, 158, 11, 0.14);
  color: #d97706;
}

.entity-badge {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 800;
  color: var(--accent);
  background: rgba(37, 99, 235, 0.08);
}

.entity-card h3 {
  margin-top: 20px;
  color: var(--text);
  font-size: 1.08rem;
  word-break: break-word;
}

.entity-desc {
  margin-top: 8px;
  min-height: 46px;
  color: var(--text-muted);
  font-size: 0.84rem;
  line-height: 1.6;
  word-break: break-word;
}

.entity-meta {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.entity-meta span {
  padding: 6px 10px;
  border-radius: 999px;
  color: var(--text-muted);
  background: var(--surface-hover);
  font-size: 0.74rem;
  font-weight: 700;
  width: fit-content;
  max-width: 100%;
}

.card-actions {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

.media-section {
  margin-top: 0;
  padding: 0;
  border: none;
  background: transparent;
  box-shadow: none;
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
  margin-top: 0;
  color: var(--text-muted);
  font-size: 0.8rem;
  font-weight: 800;
}

.back-btn {
  height: 34px;
  padding: 0 14px;
  font-weight: 700;
}

.media-title {
  margin-top: 6px;
  color: var(--text);
  font-size: 1.5rem;
  font-weight: 900;
}

.media-subtitle {
  color: var(--text-muted);
  font-size: 0.92rem;
}

.media-subtitle-row {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
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

.field {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field span {
  color: var(--text);
  font-weight: 700;
}

.field input,
.field select,
.field textarea {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--surface-hover);
  color: var(--text);
  outline: none;
  resize: vertical;
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
  .page-header,
  .media-header {
    align-items: stretch;
    flex-direction: column;
  }

  .header-actions,
  .media-toolbar {
    justify-content: flex-start;
  }

  .media-section {
    padding: 22px;
  }

  .album-tags-view.detail-mode {
    margin: -1.5rem -2.5rem 0;
  }

  .album-tags-view.detail-mode .media-section {
    padding: 0;
  }

  .media-header {
    padding: 0 2.5rem 14px;
  }

  .gallery-grid {
    padding: 0 2.5rem 2.5rem;
  }
}
</style>
