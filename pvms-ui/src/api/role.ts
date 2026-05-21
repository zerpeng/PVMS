import request from './request'

export interface RolePayload {
  roleKey: string
  roleName: string
  status: number
  superAdmin: boolean
  visitor: boolean
  menuKeys: string[]
  directoryIds: number[]
  albumIds: number[]
  tagIds: number[]
  remark?: string
}

export interface RoleInfo extends RolePayload {
  id: number
  builtin?: boolean
}

export const roleApi = {
  fetchRoles() {
    return request.get<any, RoleInfo[]>('/auth/roles')
  },

  createRole(data: RolePayload) {
    return request.post<any, RoleInfo>('/auth/roles', data)
  },

  updateRole(id: string | number, data: RolePayload) {
    return request.put<any, RoleInfo>(`/auth/roles/${id}`, data)
  },

  removeRole(id: string | number) {
    return request.delete<any, boolean>(`/auth/roles/${id}`)
  }
}
