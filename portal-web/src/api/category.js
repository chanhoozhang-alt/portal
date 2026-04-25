import request from '@/utils/request'

// 获取所有分类
export function getCategoryList() {
  return request.get('/category/list')
}

// 获取启用的分类
export function getEnabledCategories() {
  return request.get('/category/enabled')
}

// 新增分类
export function addCategory(data) {
  return request.post('/category', data)
}

// 修改分类
export function updateCategory(data) {
  return request.put('/category', data)
}

// 删除分类
export function deleteCategory(id) {
  return request.delete(`/category/${id}`)
}
