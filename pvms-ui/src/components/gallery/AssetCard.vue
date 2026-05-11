<template>
  <div class="asset-card" @click="$emit('preview')">
    <div class="card-badge">{{ asset.type }}</div>
    
    <div class="asset-thumb-container">
      <img 
        v-if="thumbnailUrl && !hasError"
        :src="thumbnailUrl" 
        class="asset-thumb" 
        loading="lazy"
        @error="handleImgError"
      />
      <!-- 加载失败或无 URL 时显示的占位符 -->
      <div 
        v-else
        class="asset-thumb-placeholder" 
        :style="{ background: hasError ? '#fee2e2' : `linear-gradient(135deg, ${asset.thumbColor}, var(--surface-hover))` }"
      >
        <ImageOff v-if="hasError" :size="32" class="error-icon" />
        <Play v-else-if="asset.type === 'MP4'" :size="32" class="play-icon" />
      </div>
      
      <div class="card-actions">
        <button class="action-btn" @click.stop="toggleHeart">
          <Heart :class="{ 'is-active': isHearted }" :fill="isHearted ? 'currentColor' : 'none'" />
        </button>
        <button class="action-btn" @click.stop="$emit('preview')">
          <Eye />
        </button>
      </div>
    </div>

    <div class="asset-info">
      <h4 class="asset-title">{{ asset.name }}</h4>
      <p class="asset-meta">
        <Calendar class="meta-icon" /> {{ asset.date }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { Heart, Eye, Calendar, ImageOff, Play } from 'lucide-vue-next'
import type { Asset } from '@/stores/media'
import { mediaApi } from '@/api/media'

const props = defineProps<{
  asset: Asset
}>()

defineEmits(['select', 'preview'])

const isHearted = ref(false)
const hasError = ref(false)
const thumbnailUrl = ref('')

const toggleHeart = () => {
  isHearted.value = !isHearted.value
}

// 释放缩略图 Blob URL，避免列表刷新或组件销毁后占用浏览器内存。
const revokeThumbnailUrl = () => {
  if (thumbnailUrl.value) {
    URL.revokeObjectURL(thumbnailUrl.value)
    thumbnailUrl.value = ''
  }
}

// 通过 axios 拉取缩略图 Blob，确保请求会携带 Authorization。
const loadThumbnail = async () => {
  hasError.value = false
  revokeThumbnailUrl()

  if (!props.asset.url) return

  try {
    const blob = await mediaApi.fetchThumbnailBlob(props.asset.url)
    thumbnailUrl.value = URL.createObjectURL(blob)
  } catch (error) {
    console.warn(`缩略图加载失败: ${props.asset.url}`, error)
    hasError.value = true
  }
}

const handleImgError = (e: Event) => {
  const target = e.target as HTMLImageElement
  console.warn(`图片加载失败: ${target.src}`)
  hasError.value = true
}

watch(() => props.asset.url, loadThumbnail, { immediate: true })

onBeforeUnmount(() => {
  revokeThumbnailUrl()
})
</script>

<style scoped>
.asset-card { position: relative; background: var(--surface); border-radius: var(--radius); padding: 14px; box-shadow: var(--card-shadow); border: 1px solid var(--border); cursor: pointer; overflow: hidden; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.asset-card:hover { transform: translateY(-8px); box-shadow: var(--card-shadow-hover); }
.card-badge { position: absolute; top: 12px; left: 12px; padding: 6px 10px; background: rgba(255, 255, 255, 0.9); backdrop-filter: blur(4px); border-radius: 12px; font-size: 0.65rem; font-weight: 700; color: #18181b; text-transform: uppercase; z-index: 10; }
[data-theme="dark"] .card-badge { background: rgba(15, 23, 42, 0.8); color: #f8fafc; }
.asset-thumb-container { position: relative; width: 100%; aspect-ratio: 1/1; border-radius: calc(var(--radius) - 10px); margin-bottom: 1.25rem; overflow: hidden; background: var(--surface-hover); display: flex; align-items: center; justify-content: center; }
.asset-thumb { width: 100%; height: 100%; object-fit: cover; transition: transform 0.5s ease; }
.asset-card:hover .asset-thumb { transform: scale(1.1); }
.asset-thumb-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; transition: all 0.3s; }
.error-icon { color: #ef4444; opacity: 0.6; }
.play-icon { color: var(--text-muted); opacity: 0.4; }
.card-actions { position: absolute; inset: 0; background: rgba(0, 0, 0, 0.1); display: flex; align-items: center; justify-content: center; gap: 12px; opacity: 0; transition: opacity 0.3s ease; backdrop-filter: blur(2px); }
.asset-card:hover .card-actions { opacity: 1; }
.action-btn { width: 40px; height: 40px; border-radius: 50%; background: white; display: flex; align-items: center; justify-content: center; border: none; cursor: pointer; color: var(--primary); box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); transition: transform 0.2s; }
.action-btn:hover { transform: scale(1.1); }
.action-btn .is-active { color: #ef4444; }
.asset-info { padding: 0 4px; }
.asset-title { font-weight: 700; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.asset-meta { margin-top: 4px; display: flex; align-items: center; gap: 4px; font-size: 0.8rem; color: var(--text-muted); }
.meta-icon { width: 12px; height: 12px; }
</style>
