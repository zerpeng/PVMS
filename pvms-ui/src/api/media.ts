import request from './request'

export interface AlbumPayload {
  name: string
  description?: string | null
  albumType?: number
  queryRule?: string | null
  coverMediaId?: number | null
}

export interface TagPayload {
  name: string
  parentId?: number
  tagType?: number
  category?: string | null
}

export const mediaApi = {
  fetchList(params?: any) {
    return request.get<any, any>('/media/files', { params })
  },

  removeMediaFile(id: string | number) {
    return request.delete<any, any>(`/media/files/${id}`)
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

  fetchAlbums() {
    return request.get<any, any>('/media/albums')
  },

  createAlbum(data: AlbumPayload) {
    return request.post<any, any>('/media/albums', data)
  },

  updateAlbum(id: string | number, data: AlbumPayload) {
    return request.put<any, any>(`/media/albums/${id}`, data)
  },

  removeAlbum(id: string | number) {
    return request.delete<any, any>(`/media/albums/${id}`)
  },

  removeAlbums(ids: Array<string | number>) {
    return request.delete<any, any>('/media/albums', { data: { ids } })
  },

  addMediaToAlbum(albumId: string | number, mediaId: string | number) {
    return request.post<any, any>(`/media/albums/${albumId}/add/${mediaId}`)
  },

  removeMediaFromAlbum(albumId: string | number, mediaId: string | number) {
    return request.delete<any, any>(`/media/albums/${albumId}/media/${mediaId}`)
  },

  fetchTags() {
    return request.get<any, any>('/media/tags')
  },

  createTag(data: TagPayload) {
    return request.post<any, any>('/media/tags', data)
  },

  updateTag(id: string | number, data: TagPayload) {
    return request.put<any, any>(`/media/tags/${id}`, data)
  },

  removeTag(id: string | number) {
    return request.delete<any, any>(`/media/tags/${id}`)
  },

  removeTags(ids: Array<string | number>) {
    return request.delete<any, any>('/media/tags', { data: { ids } })
  },

  bindTagToMedia(mediaId: string | number, tagId: string | number) {
    return request.post<any, any>('/media/tags/bind', null, {
      params: { mediaId, tagId },
    })
  },

  removeMediaFromTag(tagId: string | number, mediaId: string | number) {
    return request.delete<any, any>(`/media/tags/${tagId}/media/${mediaId}`)
  },

  addFavorite(mediaId: string | number) {
    return request.post<any, any>(`/media/favorites/${mediaId}`)
  },

  removeFavorite(mediaId: string | number) {
    return request.delete<any, any>(`/media/favorites/${mediaId}`)
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
