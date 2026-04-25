import request from '@/utils/request'

// 获取部门列表
export function getDeptList() {
  return request.get('/system/depts/list')
}

// 获取部门树
export function getDeptTree() {
  return request.get('/system/depts/tree')
}

// 新增部门
export function addDept(data) {
  return request.post('/system/depts', data)
}

// 修改部门
export function updateDept(data) {
  return request.put('/system/depts', data)
}

// 删除部门
export function deleteDept(id) {
  return request.delete(`/system/depts/${id}`)
}
