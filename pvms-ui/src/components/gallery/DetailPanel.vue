<template>
  <aside :class="['details-panel', { open: mediaStore.isDetailPanelOpen }]">
    <div class="close-btn" @click="mediaStore.closeDetailPanel">
      <X :size="18" />
    </div>
    
    <div v-if="mediaStore.selectedAsset" class="details-content">
      <!-- Preview Area -->
      <div class="preview-box" :style="{ background: `linear-gradient(45deg, ${mediaStore.selectedAsset.thumbColor}, var(--bg))` }">
        <!-- Image would go here -->
      </div>

      <!-- Title & Basic Info -->
      <div class="info-section">
        <h3 class="detail-title">{{ mediaStore.selectedAsset.name }}</h3>
        <div class="badge-group">
          <span class="type-badge">{{ mediaStore.selectedAsset.type }}</span>
          <span class="rating-badge">★ 4.8</span>
        </div>
      </div>

      <!-- Metadata Groups -->
      <div class="meta-group">
        <h5 class="group-header"><Camera :size="14" /> 相机元数据 (EXIF)</h5>
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
        </div>
      </div>

      <!-- Actions -->
      <button class="download-btn">
        <Download :size="18" /> 下载原始文件
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { X, Camera, Download } from 'lucide-vue-next'
import { useMediaStore } from '@/stores/media'

const mediaStore = useMediaStore()
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
  gap: 2.5rem;
}

.preview-box {
  width: 100%;
  aspect-ratio: 4/3;
  border-radius: 24px;
  border: 1px solid var(--border);
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
  font-size: 0.85rem;
}

.meta-row .label {
  color: var(--text-muted);
}

.meta-row .value {
  color: var(--text);
  font-weight: 500;
}

.download-btn {
  width: 100%;
  padding: 1rem;
  background: var(--accent);
  color: white;
  border: none;
  border-radius: 18px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: auto;
  transition: opacity 0.2s;
}

.download-btn:hover {
  opacity: 0.9;
}
</style>
