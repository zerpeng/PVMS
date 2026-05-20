import request from './request'

export interface UserQueryParams {
  current?: number
  size?: number
  username?: string
  nickname?: string
  roleKey?: string
  status?: number | ''
}

export interface UserPayload {
  username: string
  password?: string
  nickname?: string
  roleKey: string
  status: number
}

export interface UserInfo {
  id: number
  username: string
  nickname?: string
  roleKey: string
  status: number
  createTime?: string
  updateTime?: string
}

export const userApi = {
  fetchUsers(params: UserQueryParams) {
    return request.get<any, any>('/auth/users', { params })
  },

  createUser(data: UserPayload) {
    return request.post<any, any>('/auth/users', data)
  },

  updateUser(id: string | number, data: UserPayload) {
    return request.put<any, any>(`/auth/users/${id}`, data)
  },

  removeUser(id: string | number) {
    return request.delete<any, any>(`/auth/users/${id}`)
  },

  exportUsers(params: UserQueryParams) {
    return request.get<Blob, Blob>('/auth/users/export', {
      params,
      responseType: 'blob'
    })
  },

  importUsers(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<any, any>('/auth/users/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
