import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginByToken, loginByEmpNo, getUserInfo } from '@/api/auth'
import { setToken, removeToken } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const userInfo = ref(null)
  const menus = ref([])
  const permissions = ref([])

  // SSO 登录（上游 token）
  async function login(tokenValue) {
    const res = await loginByToken(tokenValue)
    setToken(tokenValue)
    token.value = tokenValue
    userInfo.value = res.data.user
    menus.value = res.data.menus || []
    permissions.value = res.data.permissions || []
    return res.data
  }

  // 测试登录（工号）
  async function testLogin(empNo) {
    const res = await loginByEmpNo(empNo)
    const tokenValue = res.data.tokenValue
    setToken(tokenValue)
    token.value = tokenValue
    userInfo.value = res.data.user
    menus.value = res.data.menus || []
    permissions.value = res.data.permissions || []
    return res.data
  }

  // 获取当前用户信息（刷新页面时）
  async function fetchUserInfo() {
    const res = await getUserInfo()
    userInfo.value = res.data.user
    menus.value = res.data.menus || []
    permissions.value = res.data.permissions || []
    return res.data
  }

  // 登出
  function logout() {
    token.value = ''
    userInfo.value = null
    menus.value = []
    permissions.value = []
    removeToken()
  }

  // 检查权限
  function hasPermission(perm) {
    return permissions.value.includes(perm)
  }

  return { token, userInfo, menus, permissions, login, testLogin, fetchUserInfo, logout, hasPermission }
})
