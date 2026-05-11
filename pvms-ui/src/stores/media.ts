import { defineStore } from 'pinia'

export interface Asset {
  id: string
  name: string
  type: string
  mediaType?: number
  thumbColor: string
  date: string
  url?: string
  filePath?: string
  fileSize?: number
  original?: Record<string, any>
  metadata?: {
    camera?: string
    lens?: string
    exposure?: string
    dimensions?: string
    shotTime?: string
    format?: string
    size?: string
    gps?: string
    focalLength?: string
  }
}

export const useMediaStore = defineStore('media', {
  state: () => ({
    assets: [] as Asset[],
    selectedAsset: null as Asset | null,
    isDetailPanelOpen: false,
  }),
  actions: {
    setAssets(assets: Asset[]) {
      this.assets = assets
    },
    selectAsset(asset: Asset) {
      this.selectedAsset = asset
      this.isDetailPanelOpen = true
    },
    closeDetailPanel() {
      this.isDetailPanelOpen = false
    }
  },
})
