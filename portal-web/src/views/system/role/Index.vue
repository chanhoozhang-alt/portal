<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form inline>
        <el-form-item label="角色名称">
          <el-input v-model="query.roleName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button v-permission="'system:role:add'" type="success" @click="handleAdd">新增</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="roleName" label="角色名称" width="180" />
        <el-table-column prop="roleKey" label="角色标识" width="180" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:role:edit'" size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'system:role:menu'" size="small" type="warning" @click="handleAssignMenu(row)">菜单权限</el-button>
            <el-button v-permission="'system:role:dept'" size="small" type="info" @click="handleAssignDept(row)">数据权限</el-button>
            <el-button v-permission="'system:role:delete'" size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog v-model="formDialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色名称">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="角色标识">
          <el-input v-model="form.roleKey" />
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

    <!-- 菜单权限弹窗 -->
    <el-dialog v-model="menuDialogVisible" title="分配菜单权限" width="500px">
      <el-tree
        ref="menuTreeRef"
        :data="menuList"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedMenuIds"
        :props="{ label: 'menuName', children: 'children' }"
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMenuAssign">确定</el-button>
      </template>
    </el-dialog>

    <!-- 数据权限弹窗 -->
    <el-dialog v-model="deptDialogVisible" title="分配数据权限（部门）" width="500px">
      <el-tree
        ref="deptTreeRef"
        :data="deptTree"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedDeptIds"
        :props="{ label: 'deptName', children: 'children' }"
      />
      <template #footer>
        <el-button @click="deptDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDeptAssign">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getRolePage, addRole, updateRole, deleteRole, assignRoleMenus, getRoleMenuIds, assignRoleDepts, getRoleDeptIds } from '@/api/role'
import { getMenuList } from '@/api/menu'
import { getDeptTree } from '@/api/dept'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = ref({ pageNum: 1, pageSize: 10, roleName: '' })

// 表单
const formDialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({ id: null, roleName: '', roleKey: '', sort: 0, status: 1 })

// 菜单权限
const menuDialogVisible = ref(false)
const menuTreeRef = ref(null)
const menuList = ref([])
const checkedMenuIds = ref([])
const currentRoleId = ref(null)

// 数据权限
const deptDialogVisible = ref(false)
const deptTreeRef = ref(null)
const deptTree = ref([])
const checkedDeptIds = ref([])

onMounted(() => { fetchData() })

async function fetchData() {
  loading.value = true
  try {
    const res = await getRolePage(query.value)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.value.pageNum = 1; fetchData() }
function resetQuery() { query.value = { pageNum: 1, pageSize: 10, roleName: '' }; fetchData() }

function handleAdd() {
  isEdit.value = false
  form.value = { id: null, roleName: '', roleKey: '', sort: 0, status: 1 }
  formDialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.value = { ...row }
  formDialogVisible.value = true
}

async function submitForm() {
  if (isEdit.value) {
    await updateRole(form.value)
  } else {
    await addRole(form.value)
  }
  ElMessage.success('操作成功')
  formDialogVisible.value = false
  fetchData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除角色 ${row.roleName}？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

async function handleAssignMenu(row) {
  currentRoleId.value = row.id
  const [menuRes, idsRes] = await Promise.all([getMenuList(), getRoleMenuIds(row.id)])
  // 构建树形结构
  menuList.value = buildTree(menuRes.data || [])
  checkedMenuIds.value = idsRes.data || []
  menuDialogVisible.value = true
}

async function submitMenuAssign() {
  const checkedKeys = menuTreeRef.value.getCheckedKeys()
  const halfCheckedKeys = menuTreeRef.value.getHalfCheckedKeys()
  await assignRoleMenus(currentRoleId.value, [...checkedKeys, ...halfCheckedKeys])
  ElMessage.success('分配成功')
  menuDialogVisible.value = false
}

async function handleAssignDept(row) {
  currentRoleId.value = row.id
  const [deptRes, idsRes] = await Promise.all([getDeptTree(), getRoleDeptIds(row.id)])
  deptTree.value = deptRes.data || []
  checkedDeptIds.value = idsRes.data || []
  deptDialogVisible.value = true
}

async function submitDeptAssign() {
  const checkedKeys = deptTreeRef.value.getCheckedKeys()
  await assignRoleDepts(currentRoleId.value, checkedKeys)
  ElMessage.success('分配成功')
  deptDialogVisible.value = false
}

function buildTree(list) {
  const map = {}
  const roots = []
  list.forEach(item => {
    item.children = []
    map[item.id] = item
  })
  list.forEach(item => {
    if (item.parentId && map[item.parentId]) {
      map[item.parentId].children.push(item)
    } else {
      roots.push(item)
    }
  })
  return roots
}
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }
</style>
