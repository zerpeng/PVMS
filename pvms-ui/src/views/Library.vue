<template>
  <div class="library-view">
    <div class="view-header">
      <h3 class="section-title">最近上传的媒体</h3>
      <div class="view-actions">
        <button class="filter-btn"><SlidersHorizontal :size="16" /> 筛选</button>
        <button class="filter-btn"><SortDesc :size="16" /> 排序</button>
      </div>
    </div>
    
    <div class="gallery-grid">
      <AssetCard 
        v-for="asset in mediaStore.assets" 
        :key="asset.id" 
        :asset="asset"
        @preview="openViewer(asset.id)"
      />
    </div>

    <MediaViewer
      :open="viewerOpen"
      :assets="mediaStore.assets"
      :initial-index="viewerIndex"
      @close="viewerOpen = false"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { SlidersHorizontal, SortDesc } from 'lucide-vue-next'
import AssetCard from '@/components/gallery/AssetCard.vue'
import MediaViewer from '@/components/gallery/MediaViewer.vue'
import { useMediaStore } from '@/stores/media'
import { mediaApi } from '@/api/media'

const mediaStore = useMediaStore()
const isLoading = ref(false)
const viewerOpen = ref(false)
const viewerIndex = ref(0)

// 将字节数转换为更适合界面阅读的文件大小。
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

// 打开原图/视频查看器，并定位到当前点击的媒体序号。
const openViewer = (assetId: string) => {
  const index = mediaStore.assets.findIndex(asset => asset.id === assetId)
  viewerIndex.value = index < 0 ? 0 : index
  viewerOpen.value = true
}

const loadMediaData = async () => {
  isLoading.value = true
  try {
    const response: any = await mediaApi.fetchList()
    
    // 关键修复：从 records 中提取列表
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
  } catch (error) {
    console.error('Failed to load media:', error)
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadMediaData()
})
</script>

<style scoped>
.library-view { width: 100%; }
.view-header { margin-bottom: 2rem; display: flex; justify-content: space-between; align-items: flex-end; }
.section-title { font-size: 1.1rem; color: var(--text-muted); }
.view-actions { display: flex; gap: 1rem; }
.filter-btn { display: flex; align-items: center; gap: 8px; padding: 0 1rem; height: 44px; border-radius: 12px; background: var(--surface); border: 1px solid var(--border); color: var(--text); font-size: 0.85rem; cursor: pointer; transition: all 0.2s; }
.filter-btn:hover { background: var(--surface-hover); transform: translateY(-2px); }
.gallery-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 2.5rem; }
</style>
