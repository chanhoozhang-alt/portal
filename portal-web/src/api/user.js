import request from '@/utils/request'

// 分页查询用户
export function getUserPage(params) {
  return request.get('/system/users/page', { params })
}

// 根据工号查询用户
export function getUserByEmpNo(empNo) {
  return request.get('/system/users/byEmpNo', { params: { empNo } })
}

// 更新用户状态
export function updateUserStatus(id, status) {
  return request.put(`/system/users/${id}/status`, null, { params: { status } })
}

// 分配用户角色
export function assignUserRoles(id, roleIds) {
  return request.post(`/system/users/${id}/roles`, roleIds)
}

// 获取用户角色列表
export function getUserRoles(id) {
  return request.get(`/system/users/${id}/roles`)
}
