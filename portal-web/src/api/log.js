import request from '@/utils/request'

// 分页查询操作日志
export function getLogPage(params) {
  return request.get('/system/logs/page', { params })
}
