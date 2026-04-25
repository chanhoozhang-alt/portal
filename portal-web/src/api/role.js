import request from '@/utils/request'

// 分页查询角色
export function getRolePage(params) {
  return request.get('/system/roles/page', { params })
}

// 查询所有角色（下拉选择用）
export function getRoleList() {
  return request.get('/system/roles/list')
}

// 新增角色
export function addRole(data) {
  return request.post('/system/roles', data)
}

// 修改角色
export function updateRole(data) {
  return request.put('/system/roles', data)
}

// 删除角色
export function deleteRole(id) {
  return request.delete(`/system/roles/${id}`)
}

// 分配角色菜单
export function assignRoleMenus(id, menuIds) {
  return request.post(`/system/roles/${id}/menus`, menuIds)
}

// 获取角色已分配的菜单 ID
export function getRoleMenuIds(id) {
  return request.get(`/system/roles/${id}/menus`)
}

// 分配角色数据权限（部门）
export function assignRoleDepts(id, deptIds) {
  return request.post(`/system/roles/${id}/depts`, deptIds)
}

// 获取角色已分配的部门 ID
export function getRoleDeptIds(id) {
  return request.get(`/system/roles/${id}/depts`)
}
