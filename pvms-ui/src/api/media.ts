import request from './request'

export const mediaApi = {
  fetchList(params?: any) {
    return request.get<any, any>('/media/files', { params })
  },

  fetchDirectories() {
    return request.get<any, any>('/media/directories')
  },

  addDirectory(data: any) {
    return request.post<any, any>('/media/directories', data)
  },

  scanDirectories(directoryIds: Array<string | number>) {
    return request.post<any, any>('/media/directories/scan', { directoryIds })
  },

  removeDirectory(id: string | number) {
    return request.delete<any, any>(`/media/directories/${id}`)
  },

  fetchPreference() {
    return request.get<any, any>('/media/preferences')
  },

  updateSyncScanPreference(syncScanEnabled: boolean) {
    return request.put<any, any>('/media/preferences/sync-scan', { syncScanEnabled })
  },

  fetchOriginalBlob(id: string | number) {
    return request.get<Blob, Blob>(`/media/files/${id}/content`, {
      responseType: 'blob'
    })
  },

  fetchThumbnailBlob(path: string) {
    return request.get<Blob, Blob>(this.getThumbnailUrl(path), {
      responseType: 'blob'
    })
  },

  getThumbnailUrl(path: string) {
    if (!path) return ''
    const fileName = path.split('\\').pop() || path.split('/').pop()
    return `/media/thumbnails/${fileName}`
  },

  fetchStorageStats() {
    return request.get<any, any>('/system/storage')
  }
}
