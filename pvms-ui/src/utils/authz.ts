export interface AuthInfo {
  username?: string
  nickname?: string
  roleKey: string
  roleName?: string
  superAdmin: boolean
  visitor: boolean
  menuKeys: string[]
  directoryIds: number[]
  albumIds: number[]
  tagIds: number[]
}

const splitStrings = (value: unknown) => {
  if (Array.isArray(value)) return value.map(String)
  if (typeof value !== 'string' || !value.trim()) return []
  return value.split(',').map(item => item.trim()).filter(Boolean)
}

const splitNumbers = (value: unknown) => splitStrings(value).map(Number).filter(Number.isFinite)

export const getAuthInfo = (): AuthInfo => {
  const token = localStorage.getItem('token')
  if (!token) {
    return { roleKey: 'USER', superAdmin: false, visitor: false, menuKeys: [], directoryIds: [], albumIds: [], tagIds: [] }
  }

  try {
    const payload = JSON.parse(decodeURIComponent(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')).split('').map(char => `%${(`00${char.charCodeAt(0).toString(16)}`).slice(-2)}`).join('')))
    const roleKey = payload.roleKey || splitStrings(payload.roles)[0]?.replace('ROLE_', '') || 'USER'
    return {
      username: payload.sub,
      nickname: payload.nickname,
      roleKey,
      roleName: payload.roleName,
      superAdmin: Boolean(payload.superAdmin) || roleKey === 'ADMIN',
      visitor: Boolean(payload.visitor) || roleKey === 'VISITOR',
      menuKeys: splitStrings(payload.menuKeys),
      directoryIds: splitNumbers(payload.directoryIds),
      albumIds: splitNumbers(payload.albumIds),
      tagIds: splitNumbers(payload.tagIds)
    }
  } catch (error) {
    return { roleKey: 'USER', superAdmin: false, visitor: false, menuKeys: [], directoryIds: [], albumIds: [], tagIds: [] }
  }
}

export const canShowMenu = (menuKey: string) => {
  const auth = getAuthInfo()
  if (auth.superAdmin) return true
  return auth.menuKeys.length === 0 ? !['users', 'roles', 'applications'].includes(menuKey) : auth.menuKeys.includes(menuKey)
}
