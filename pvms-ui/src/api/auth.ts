import request from './request'

export const authApi = {
  /**
   * 登录
   */
  login(data: any) {
    return request.post<any, any>('/auth/login', data)
  },

  /**
   * 注册申请
   */
  register(data: any) {
    return request.post<any, any>('/auth/register', data)
  },

  /**
   * 获取验证码
   */
  getCaptcha() {
    return request.get<any, any>('/auth/captcha')
  }
}
