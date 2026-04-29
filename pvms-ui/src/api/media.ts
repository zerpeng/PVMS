import request from './request'
import type { Asset } from '@/stores/media'

export const mediaApi = {
  /**
   * 获取媒体文件列表
   */
  fetchList(params?: any) {
    return request.get<any, any>('/media/files', { params })
  },

  /**
   * 获取缩略图预览地址 (待确定的网关映射路径)
   */
  getThumbnailUrl(path: string) {
    if (!path) return ''
    const fileName = path.split('\\').pop() || path.split('/').pop()
    
    // 请根据浏览器测试结果修改下方的基础路径
    // 方案 A: /api/thumbnails/ (当前报错)
    // 方案 B: /thumbnails/ (不带 api)
    // 方案 C: /api/media/thumbnails/
    const baseUrl = '/api/media/thumbnails' 
    
    return `${baseUrl}/${fileName}`
  },

  /**
   * 获取系统存储状态
   */
  fetchStorageStats() {
    return request.get<any, any>('/system/storage')
  }
}
