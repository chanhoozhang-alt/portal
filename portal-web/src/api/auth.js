import request from '@/utils/request'

// SSO 登录（上游传入 token）
export function loginByToken(token) {
  return request.post('/auth/verify', { token })
}

// 测试登录（通过工号登录，仅开发环境使用）
export function loginByEmpNo(empNo) {
  return request.post('/auth/test-login', { empNo })
}

// 获取当前用户信息（菜单 + 权限）
export function getUserInfo() {
  return request.get('/auth/permissions')
}

// 获取当前用户菜单树
export function getMenuTree() {
  return request.get('/auth/menus')
}

// 登出
export function logout() {
  return request.post('/auth/logout')
}
