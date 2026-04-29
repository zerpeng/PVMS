import { defineStore } from 'pinia'

export interface Asset {
  id: string
  name: string
  type: 'RAW' | 'JPG' | 'PNG' | 'MOV'
  thumbColor: string
  date: string
  metadata?: {
    camera?: string
    lens?: string
    exposure?: string
    dimensions?: string
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
