import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/store/user'
import { generateRoutes } from './dynamic'

NProgress.configure({ showSpinner: false })

// 基础路由（无需权限）
const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/404.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes
})

const whiteList = ['/login']

let hasAddedRoutes = false

router.beforeEach(async (to, from, next) => {
  NProgress.start()
  const token = getToken()

  if (token) {
    if (to.path === '/login') {
      next({ path: '/' })
    } else {
      const userStore = useUserStore()
      if (userStore.userInfo && hasAddedRoutes) {
        next()
      } else {
        try {
          if (!userStore.userInfo) {
            await userStore.fetchUserInfo()
          }
          if (!hasAddedRoutes) {
            const dynamicRoutes = generateRoutes(userStore.menus)
            dynamicRoutes.forEach(route => router.addRoute(route))
            // 首页兜底路由
            router.addRoute({
              path: '/',
              component: () => import('@/layout/Main.vue'),
              redirect: '/portal',
              children: []
            })
            router.addRoute({ path: '/:pathMatch(.*)*', redirect: '/404' })
            hasAddedRoutes = true
          }
          next({ ...to, replace: true })
        } catch (e) {
          userStore.logout()
          next('/login')
        }
      }
    }
  } else {
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next('/login')
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

export function resetRouter() {
  hasAddedRoutes = false
  const currentRoutes = router.getRoutes()
  currentRoutes.forEach(route => {
    if (route.name && !['Login', 'NotFound'].includes(route.name)) {
      router.removeRoute(route.name)
    }
  })
}

export default router
