<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.not-found {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}
.not-found h1 {
  font-size: 72px;
  margin: 0;
  color: #409eff;
}
.not-found p {
  font-size: 18px;
  margin: 16px 0 24px;
}
</style>
