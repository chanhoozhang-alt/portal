import { useUserStore } from '@/store/user'

export default {
  install(app) {
    app.directive('permission', {
      mounted(el, binding) {
        const userStore = useUserStore()
        const requiredPerm = binding.value
        if (requiredPerm && !userStore.hasPermission(requiredPerm)) {
          el.parentNode?.removeChild(el)
        }
      }
    })
  }
}
