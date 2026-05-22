import request from './request'

export interface VisitorApplication {
  id: number
  userId?: number
  username: string
  applicantName: string
  reason?: string
  targetAlbumId?: number
  status: number
  statusText: string
  roleKey?: string
  auditUsername?: string
  auditRemark?: string
  createTime?: string
  auditTime?: string
}

export interface AuditPayload {
  approved: boolean
  roleName?: string
  superAdmin: boolean
  visitor: boolean
  menuKeys: string[]
  directoryIds: number[]
  albumIds: number[]
  tagIds: number[]
  remark?: string
}

export const applicationApi = {
  fetchApplications(status?: number | '') {
    return request.get<any, VisitorApplication[]>('/auth/applications', {
      params: status === '' || status === undefined ? {} : { status }
    })
  },

  fetchPendingCount() {
    return request.get<any, { count: number }>('/auth/applications/pending-count')
  },

  auditApplication(id: string | number, data: AuditPayload) {
    return request.put<any, VisitorApplication>(`/auth/applications/${id}/audit`, data)
  }
}
