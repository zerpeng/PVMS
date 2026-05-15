<template>
  <transition name="viewer-fade">
    <div v-if="open && activeAsset" class="viewer-overlay">
      <div class="viewer-shell">
        <section class="media-stage">
          <header class="viewer-toolbar">
            <button class="icon-btn" type="button" @click="$emit('close')" title="关闭">
              <X :size="20" />
            </button>
            <div class="viewer-counter">{{ currentIndex + 1 }} / {{ assets.length }}</div>
            <div class="tool-group">
              <button class="icon-btn" type="button" @click="zoomOut" :disabled="!isImage" title="缩小">
                <ZoomOut :size="18" />
              </button>
              <span class="zoom-label">{{ Math.round(zoom * 100) }}%</span>
              <button class="icon-btn" type="button" @click="zoomIn" :disabled="!isImage" title="放大">
                <ZoomIn :size="18" />
              </button>
            </div>
          </header>

          <button class="nav-btn prev" type="button" @click="showPrev" :disabled="!hasPrev" title="上一项">
            <ChevronLeft :size="28" />
          </button>

          <div class="media-canvas">
            <div v-if="loading" class="viewer-loading">加载中...</div>
            <video
              v-else-if="isVideo && objectUrl"
              :src="objectUrl"
              class="media-video"
              controls
              autoplay
            />
            <img
              v-else-if="objectUrl"
              :src="objectUrl"
              :class="['media-image', { zoomed: zoom > 1 }]"
              :style="{ transform: `scale(${zoom})` }"
              alt="原始媒体"
            />
            <div v-else class="viewer-empty">无法加载原始文件</div>
          </div>

          <button class="nav-btn next" type="button" @click="showNext" :disabled="!hasNext" title="下一项">
            <ChevronRight :size="28" />
          </button>
        </section>

        <aside class="viewer-meta">
          <div>
            <p class="meta-kicker">{{ activeAsset.type }}</p>
            <h2 class="meta-title">{{ activeAsset.name }}</h2>
            <p class="meta-path">{{ activeAsset.filePath || '未记录原始路径' }}</p>
          </div>

          <div class="meta-actions">
            <button class="meta-btn primary" type="button" @click="$emit('assign-to-album', activeAsset.id)">
              <FolderPlus :size="16" /> 添加到相册
            </button>
            <button class="meta-btn" type="button" @click="$emit('assign-to-tag', activeAsset.id)">
              <Tags :size="16" /> 添加到标签
            </button>
          </div>

          <div class="meta-list">
            <div v-for="item in metaRows" :key="item.label" class="meta-item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ChevronLeft, ChevronRight, FolderPlus, Tags, X, ZoomIn, ZoomOut } from 'lucide-vue-next'
import type { Asset } from '@/stores/media'
import { mediaApi } from '@/api/media'

const props = defineProps<{
  open: boolean
  assets: Asset[]
  initialIndex: number
}>()

defineEmits(['close', 'assign-to-album', 'assign-to-tag'])

const currentIndex = ref(0)
const zoom = ref(1)
const loading = ref(false)
const objectUrl = ref('')

const activeAsset = computed(() => props.assets[currentIndex.value])
const hasPrev = computed(() => currentIndex.value > 0)
const hasNext = computed(() => currentIndex.value < props.assets.length - 1)
const isVideo = computed(() => activeAsset.value?.mediaType === 2 || ['MP4', 'MOV', 'AVI', 'MKV', 'FLV'].includes(activeAsset.value?.type || ''))
const isImage = computed(() => !!activeAsset.value && !isVideo.value)

const metaRows = computed(() => {
  const asset = activeAsset.value
  if (!asset) return []

  return [
    { label: '拍摄时间', value: asset.metadata?.shotTime || asset.date || '未知' },
    { label: '相机', value: asset.metadata?.camera || '未知' },
    { label: '镜头', value: asset.metadata?.lens || '未知' },
    { label: '曝光', value: asset.metadata?.exposure || '无数据' },
    { label: '焦距', value: asset.metadata?.focalLength || '无数据' },
    { label: '分辨率', value: asset.metadata?.dimensions || '无数据' },
    { label: '文件大小', value: asset.metadata?.size || '未知' },
    { label: 'GPS', value: asset.metadata?.gps || '无数据' },
    { label: '原始格式', value: asset.metadata?.format || asset.type }
  ]
})

const revokeObjectUrl = () => {
  if (objectUrl.value) {
    URL.revokeObjectURL(objectUrl.value)
    objectUrl.value = ''
  }
}

const loadActiveAsset = async () => {
  const asset = activeAsset.value
  if (!asset) return

  loading.value = true
  zoom.value = 1
  revokeObjectUrl()

  try {
    const blob = await mediaApi.fetchOriginalBlob(asset.id)
    objectUrl.value = URL.createObjectURL(blob)
  } catch (error) {
    console.error('Failed to load original media:', error)
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.open, props.initialIndex, props.assets.length] as const,
  () => {
    if (!props.open) {
      revokeObjectUrl()
      return
    }
    if (props.assets.length === 0) return
    currentIndex.value = Math.min(Math.max(props.initialIndex, 0), props.assets.length - 1)
    loadActiveAsset()
  },
  { immediate: true }
)

watch(currentIndex, () => {
  if (props.open) {
    loadActiveAsset()
  }
})

const showPrev = () => {
  if (hasPrev.value) currentIndex.value -= 1
}

const showNext = () => {
  if (hasNext.value) currentIndex.value += 1
}

const zoomIn = () => {
  if (isImage.value) zoom.value = Math.min(zoom.value + 0.25, 4)
}

const zoomOut = () => {
  if (isImage.value) zoom.value = Math.max(zoom.value - 0.25, 0.5)
}

onBeforeUnmount(() => {
  revokeObjectUrl()
})
</script>

<style scoped>
.viewer-overlay {
  position: fixed;
  inset: 0;
  z-index: 300;
  padding: 18px;
  background: rgba(2, 6, 23, 0.78);
  backdrop-filter: blur(10px);
}

.viewer-shell {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.18);
  border-radius: 24px;
  background: #0f172a;
  box-shadow: 0 30px 100px rgba(0, 0, 0, 0.38);
}

.media-stage {
  position: relative;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.viewer-toolbar {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  color: #f8fafc;
}

.icon-btn,
.nav-btn {
  border: none;
  color: #f8fafc;
  background: rgba(255, 255, 255, 0.12);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.icon-btn {
  width: 38px;
  height: 38px;
  border-radius: 999px;
}

.icon-btn:disabled,
.nav-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.viewer-counter {
  font-weight: 800;
  letter-spacing: 0;
}

.tool-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.zoom-label {
  width: 52px;
  text-align: center;
  font-size: 0.85rem;
  color: #cbd5e1;
}

.media-canvas {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px 74px 28px;
}

.media-image {
  display: block;
  width: auto;
  height: auto;
  max-width: 100%;
  max-height: calc(100vh - 146px);
  object-fit: contain;
  transform-origin: center;
  transition: transform 0.18s ease;
}

.media-image.zoomed {
  max-width: none;
  max-height: none;
}

.media-video {
  width: min(100%, 1080px);
  max-height: calc(100vh - 146px);
  background: #000;
  border-radius: 12px;
  object-fit: contain;
}

.viewer-loading,
.viewer-empty {
  color: #cbd5e1;
  font-weight: 700;
}

.nav-btn {
  position: absolute;
  top: 50%;
  width: 48px;
  height: 68px;
  border-radius: 999px;
  transform: translateY(-50%);
}

.nav-btn.prev {
  left: 18px;
}

.nav-btn.next {
  right: 18px;
}

.viewer-meta {
  display: flex;
  flex-direction: column;
  gap: 28px;
  padding: 32px;
  overflow-y: auto;
  color: var(--text);
  background: var(--surface);
}

.meta-kicker {
  margin-bottom: 8px;
  color: var(--accent);
  font-size: 0.75rem;
  font-weight: 900;
}

.meta-title {
  color: var(--text);
  font-size: 1.35rem;
  line-height: 1.25;
  word-break: break-word;
}

.meta-path {
  margin-top: 10px;
  color: var(--text-muted);
  font-size: 0.8rem;
  line-height: 1.5;
  word-break: break-all;
}

.meta-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meta-btn {
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 14px;
  border: 1px solid var(--border);
  color: var(--text);
  background: var(--surface-hover);
  font-weight: 800;
  cursor: pointer;
}

.meta-btn.primary {
  border: none;
  color: #fff;
  background: var(--accent);
}

.meta-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meta-item {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
  color: var(--text-muted);
  font-size: 0.86rem;
}

.meta-item strong {
  color: var(--text);
  text-align: right;
  font-weight: 700;
  word-break: break-word;
}

.viewer-fade-enter-active,
.viewer-fade-leave-active {
  transition: opacity 0.22s ease;
}

.viewer-fade-enter-from,
.viewer-fade-leave-to {
  opacity: 0;
}

@media (max-width: 900px) {
  .viewer-overlay {
    padding: 0;
  }

  .viewer-shell {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(0, 1fr) auto;
    border-radius: 0;
  }

  .media-canvas {
    padding: 16px 56px 22px;
  }

  .media-image,
  .media-video {
    max-height: calc(62vh - 86px);
  }

  .viewer-meta {
    max-height: 38vh;
  }
}
</style>
