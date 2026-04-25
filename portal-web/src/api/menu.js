import request from '@/utils/request'

// 获取菜单列表
export function getMenuList() {
  return request.get('/system/menus/list')
}

// 新增菜单
export function addMenu(data) {
  return request.post('/system/menus', data)
}

// 修改菜单
export function updateMenu(data) {
  return request.put('/system/menus', data)
}

// 删除菜单
export function deleteMenu(id) {
  return request.delete(`/system/menus/${id}`)
}
