<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form inline>
        <el-form-item label="应用名称">
          <el-input v-model="query.appName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button v-permission="'portal:app:add'" type="success" @click="handleAdd">新增</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="appName" label="应用名称" width="180" />
        <el-table-column prop="appUrl" label="应用地址" />
        <el-table-column prop="appDesc" label="描述" />
        <el-table-column prop="categoryId" label="分类" width="120">
          <template #default="{ row }">
            {{ getCategoryName(row.categoryId) }}
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'portal:app:edit'" size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'portal:app:delete'" size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchData"
        @current-change="fetchData"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formDialogVisible" :title="isEdit ? '编辑应用' : '新增应用'" width="550px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="应用名称">
          <el-input v-model="form.appName" />
        </el-form-item>
        <el-form-item label="应用地址">
          <el-input v-model="form.appUrl" />
        </el-form-item>
        <el-form-item label="应用图标">
          <el-input v-model="form.appIcon" />
        </el-form-item>
        <el-form-item label="应用描述">
          <el-input v-model="form.appDesc" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="所属分类">
          <el-select v-model="form.categoryId" placeholder="请选择">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAppPage, addApp, updateApp, deleteApp } from '@/api/app'
import { getCategoryList } from '@/api/category'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = ref({ pageNum: 1, pageSize: 10, appName: '', status: null })
const categories = ref([])

const formDialogVisible = ref(false)
const isEdit = ref(false)
const form = ref(getDefaultForm())

function getDefaultForm() {
  return { id: null, appName: '', appUrl: '', appIcon: '', appDesc: '', categoryId: null, sort: 0, status: 1 }
}

onMounted(() => {
  fetchData()
  fetchCategories()
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getAppPage(query.value)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  const res = await getCategoryList()
  categories.value = res.data || []
}

function getCategoryName(id) {
  return categories.value.find(c => c.id === id)?.categoryName || '-'
}

function handleSearch() { query.value.pageNum = 1; fetchData() }
function resetQuery() { query.value = { pageNum: 1, pageSize: 10, appName: '', status: null }; fetchData() }

function handleAdd() {
  isEdit.value = false
  form.value = getDefaultForm()
  formDialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.value = { ...row }
  formDialogVisible.value = true
}

async function submitForm() {
  if (isEdit.value) {
    await updateApp(form.value)
  } else {
    await addApp(form.value)
  }
  ElMessage.success('操作成功')
  formDialogVisible.value = false
  fetchData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除应用 ${row.appName}？`, '提示', { type: 'warning' })
  await deleteApp(row.id)
  ElMessage.success('删除成功')
  fetchData()
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }
</style>
