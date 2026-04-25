import MainLayout from '@/layout/Main.vue'

// 后端菜单 -> 前端路由 的页面组件映射
const viewModules = import.meta.glob('../views/**/*.vue')

/**
 * 根据后端菜单树生成前端路由
 */
export function generateRoutes(menus) {
  const routes = []

  for (const menu of menus) {
    if (menu.menuType === 'B') continue // 按钮不生成路由

    const route = {
      path: menu.path,
      name: routeName(menu),
      meta: {
        title: menu.menuName,
        icon: menu.icon,
        permission: menu.permission
      }
    }

    if (menu.children && menu.children.length > 0) {
      // 目录：有子菜单
      route.component = MainLayout
      route.redirect = `${menu.path}/${menu.children[0].path}`
      route.children = menu.children
        .filter(child => child.menuType !== 'B')
        .map(child => buildChildRoute(child))
    } else {
      // 菜单：叶子节点，挂在根 layout 下
      route.component = MainLayout
      route.children = [{
        path: '',
        name: routeName(menu) + '_index',
        component: resolveComponent(menu.component),
        meta: { title: menu.menuName, icon: menu.icon, permission: menu.permission }
      }]
    }

    routes.push(route)
  }

  return routes
}

function buildChildRoute(menu) {
  return {
    path: menu.path,
    name: routeName(menu),
    component: resolveComponent(menu.component),
    meta: {
      title: menu.menuName,
      icon: menu.icon,
      permission: menu.permission
    },
    children: menu.children && menu.children.length > 0
      ? menu.children.filter(c => c.menuType !== 'B').map(c => buildChildRoute(c))
      : undefined
  }
}

function resolveComponent(component) {
  if (!component) return undefined
  const path = `../views/${component}.vue`
  return viewModules[path] || (() => import('@/views/404.vue'))
}

function routeName(menu) {
  return (menu.path || '') + '_' + menu.id
}
