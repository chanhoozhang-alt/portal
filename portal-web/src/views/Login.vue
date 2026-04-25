<template>
  <div class="login-container">
    <div class="login-card">
      <h2>门户管理系统</h2>

      <!-- SSO 自动登录中 -->
      <div v-if="ssoLoading">
        <p>正在登录中...</p>
      </div>

      <!-- SSO 错误或无 token 时，显示工号登录表单 -->
      <div v-else-if="showLoginForm">
        <p class="tip">开发测试模式，请输入工号登录</p>
        <el-input
          v-model="empNo"
          placeholder="请输入工号"
          size="large"
          @keyup.enter="handleLogin"
          style="margin-bottom: 16px"
        >
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
        <el-button
          type="primary"
          size="large"
          :loading="btnLoading"
          style="width: 100%"
          @click="handleLogin"
        >
          登 录
        </el-button>
        <p class="accounts-tip">
          测试账号：admin（管理员）/ zhangsan / lisi
        </p>
      </div>

      <!-- SSO 登录失败提示 -->
      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { extractTokenFromUrl, getToken } from '@/utils/auth'

const router = useRouter()
const userStore = useUserStore()

const ssoLoading = ref(false)
const showLoginForm = ref(false)
const empNo = ref('')
const btnLoading = ref(false)
const errorMsg = ref('')

onMounted(async () => {
  // 优先从 URL 取 token（SSO 跳转场景）
  let token = extractTokenFromUrl()

  // 其次从 localStorage 取（刷新页面场景）
  if (!token) {
    token = getToken()
  }

  if (token) {
    ssoLoading.value = true
    try {
      await userStore.login(token)
      router.push('/')
    } catch (e) {
      errorMsg.value = 'SSO 登录失败，请使用工号登录'
      showLoginForm.value = true
    } finally {
      ssoLoading.value = false
    }
    return
  }

  // 无 token，显示工号登录表单
  showLoginForm.value = true
})

async function handleLogin() {
  if (!empNo.value.trim()) {
    ElMessage.warning('请输入工号')
    return
  }
  btnLoading.value = true
  errorMsg.value = ''
  try {
    await userStore.testLogin(empNo.value.trim())
    router.push('/')
  } catch (e) {
    errorMsg.value = '登录失败：' + (e.message || '未知错误')
  } finally {
    btnLoading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  background: #fff;
  padding: 40px;
  border-radius: 8px;
  text-align: center;
  min-width: 380px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}
.login-card h2 {
  margin-bottom: 24px;
  color: #333;
}
.tip {
  color: #909399;
  margin-bottom: 20px;
  font-size: 14px;
}
.error {
  color: #f56c6c;
  margin-top: 12px;
  font-size: 14px;
}
.accounts-tip {
  margin-top: 16px;
  color: #c0c4cc;
  font-size: 12px;
}
</style>
