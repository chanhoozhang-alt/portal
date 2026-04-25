<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-button v-permission="'system:dept:add'" type="success" @click="handleAdd(null)">新增部门</el-button>
    </el-card>

    <el-card>
      <el-table :data="deptTree" v-loading="loading" border row-key="id" :tree-props="{ children: 'children' }" default-expand-all>
        <el-table-column prop="deptName" label="部门名称" width="250" />
        <el-table-column prop="sort" label="排序" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:dept:add'" size="small" type="success" @click="handleAdd(row)">新增</el-button>
            <el-button v-permission="'system:dept:edit'" size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'system:dept:delete'" size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formDialogVisible" :title="isEdit ? '编辑部门' : '新增部门'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="上级部门">
          <el-input :value="parentName" disabled />
        </el-form-item>
        <el-form-item label="部门名称">
          <el-input v-model="form.deptName" />
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
import { getDeptList, addDept, updateDept, deleteDept } from '@/api/dept'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const deptTree = ref([])

const formDialogVisible = ref(false)
const isEdit = ref(false)
const parentName = ref('根部门')
const form = ref(getDefaultForm())

function getDefaultForm() {
  return { id: null, deptName: '', parentId: 0, sort: 0, status: 1 }
}

onMounted(() => { fetchData() })

async function fetchData() {
  loading.value = true
  try {
    const res = await getDeptList()
    deptTree.value = buildTree(res.data || [])
  } finally {
    loading.value = false
  }
}

function handleAdd(parent) {
  isEdit.value = false
  form.value = getDefaultForm()
  if (parent) {
    form.value.parentId = parent.id
    parentName.value = parent.deptName
  } else {
    form.value.parentId = 0
    parentName.value = '根部门'
  }
  formDialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.value = { ...row }
  const parent = findDeptById(deptTree.value, row.parentId)
  parentName.value = parent ? parent.deptName : '根部门'
  formDialogVisible.value = true
}

async function submitForm() {
  if (isEdit.value) {
    await updateDept(form.value)
  } else {
    await addDept(form.value)
  }
  ElMessage.success('操作成功')
  formDialogVisible.value = false
  fetchData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除部门 ${row.deptName}？`, '提示', { type: 'warning' })
  await deleteDept(row.id)
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

function findDeptById(tree, id) {
  for (const item of tree) {
    if (item.id === id) return item
    if (item.children) {
      const found = findDeptById(item.children, id)
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
