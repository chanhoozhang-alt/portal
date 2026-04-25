<template>
  <div class="portal-home">
    <div v-for="category in categories" :key="category.id" class="category-section">
      <h3 class="category-title">{{ category.categoryName }}</h3>
      <el-row :gutter="20">
        <el-col v-for="app in getAppsByCategory(category.id)" :key="app.id" :span="6">
          <el-card shadow="hover" class="app-card" @click="openApp(app)">
            <div class="app-icon">
              <el-icon :size="40"><component :is="app.appIcon || 'Monitor'" /></el-icon>
            </div>
            <div class="app-info">
              <h4>{{ app.appName }}</h4>
              <p>{{ app.appDesc }}</p>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
    <el-empty v-if="categories.length === 0" description="暂无应用" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getEnabledCategories } from '@/api/category'
import { getEnabledApps } from '@/api/app'

const categories = ref([])
const apps = ref([])

onMounted(async () => {
  const [catRes, appRes] = await Promise.all([getEnabledCategories(), getEnabledApps()])
  categories.value = catRes.data || []
  apps.value = appRes.data || []
})

function getAppsByCategory(categoryId) {
  return apps.value.filter(app => app.categoryId === categoryId && app.status === 1)
}

function openApp(app) {
  if (app.appUrl) {
    window.open(app.appUrl, '_blank')
  }
}
</script>

<style scoped>
.category-section {
  margin-bottom: 30px;
}
.category-title {
  margin-bottom: 16px;
  padding-left: 10px;
  border-left: 3px solid #409eff;
  color: #333;
}
.app-card {
  cursor: pointer;
  margin-bottom: 16px;
  text-align: center;
  transition: transform 0.2s;
}
.app-card:hover {
  transform: translateY(-4px);
}
.app-icon {
  margin-bottom: 10px;
  color: #409eff;
}
.app-info h4 {
  margin: 0 0 6px;
  font-size: 15px;
}
.app-info p {
  margin: 0;
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
