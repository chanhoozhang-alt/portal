<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-button v-permission="'system:menu:add'" type="success" @click="handleAdd(null)">新增菜单</el-button>
    </el-card>

    <el-card>
      <el-table :data="menuTree" v-loading="loading" border row-key="id" :tree-props="{ children: 'children' }" default-expand-all>
        <el-table-column prop="menuName" label="菜单名称" width="200" />
        <el-table-column prop="icon" label="图标" width="80">
          <template #default="{ row }">
            <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="menuType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.menuType === 'D'" type="">目录</el-tag>
            <el-tag v-else-if="row.menuType === 'M'" type="success">菜单</el-tag>
            <el-tag v-else-if="row.menuType === 'B'" type="warning">按钮</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" width="180" />
        <el-table-column prop="component" label="组件路径" width="200" />
        <el-table-column prop="permission" label="权限标识" width="200" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.menuType !== 'B'" v-permission="'system:menu:add'" size="small" type="success" @click="handleAdd(row)">新增</el-button>
            <el-button v-permission="'system:menu:edit'" size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'system:menu:delete'" size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formDialogVisible" :title="isEdit ? '编辑菜单' : '新增菜单'" width="550px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="上级菜单">
          <el-input :value="parentName" disabled />
        </el-form-item>
        <el-form-item label="菜单类型">
          <el-radio-group v-model="form.menuType">
            <el-radio value="D">目录</el-radio>
            <el-radio value="M">菜单</el-radio>
            <el-radio value="B">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称">
          <el-input v-model="form.menuName" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'B'" label="路由路径">
          <el-input v-model="form.path" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 'M'" label="组件路径">
          <el-input v-model="form.component" placeholder="如 system/user/Index" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'B'" label="图标">
          <el-input v-model="form.icon" />
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="form.permission" placeholder="如 system:user:add" />
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
import { ref, computed, onMounted } from 'vue'
import { getMenuList, addMenu, updateMenu, deleteMenu } from '@/api/menu'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const menuTree = ref([])

// 表单
const formDialogVisible = ref(false)
const isEdit = ref(false)
const parentName = ref('根目录')
const form = ref(getDefaultForm())

function getDefaultForm() {
  return { id: null, menuName: '', parentId: 0, path: '', component: '', icon: '', menuType: 'M', permission: '', sort: 0, status: 1 }
}

onMounted(() => { fetchData() })

async function fetchData() {
  loading.value = true
  try {
    const res = await getMenuList()
    menuTree.value = buildTree(res.data || [])
  } finally {
    loading.value = false
  }
}

function handleAdd(parent) {
  isEdit.value = false
  form.value = getDefaultForm()
  if (parent) {
    form.value.parentId = parent.id
    parentName.value = parent.menuName
  } else {
    form.value.parentId = 0
    parentName.value = '根目录'
  }
  formDialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.value = { ...row }
  // 查找父菜单名称
  const parent = findMenuById(menuTree.value, row.parentId)
  parentName.value = parent ? parent.menuName : '根目录'
  formDialogVisible.value = true
}

async function submitForm() {
  if (isEdit.value) {
    await updateMenu(form.value)
  } else {
    await addMenu(form.value)
  }
  ElMessage.success('操作成功')
  formDialogVisible.value = false
  fetchData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除菜单 ${row.menuName}？`, '提示', { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

function buildTree(list) {
  const map = {}
  const roots = []
  list.forEach(item => { item.children = []; map[item.id] = item })
  list.forEach(item => {
    if (item.parentId && map[item.parentId]) {
      map[item.parentId].children.push(item)
    } else {
      roots.push(item)
    }
  })
  return roots
}

function findMenuById(tree, id) {
  for (const item of tree) {
    if (item.id === id) return item
    if (item.children) {
      const found = findMenuById(item.children, id)
      if (found) return found
    }
  }
  return null
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }
</style>
