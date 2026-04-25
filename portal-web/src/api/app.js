import request from '@/utils/request'

// 分页查询应用
export function getAppPage(params) {
  return request.get('/app/page', { params })
}

// 查询所有启用的应用（门户首页）
export function getEnabledApps() {
  return request.get('/app/list')
}

// 新增应用
export function addApp(data) {
  return request.post('/app', data)
}

// 修改应用
export function updateApp(data) {
  return request.put('/app', data)
}

// 删除应用
export function deleteApp(id) {
  return request.delete(`/app/${id}`)
}
