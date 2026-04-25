<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form inline>
        <el-form-item label="操作人">
          <el-input v-model="query.username" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="操作">
          <el-input v-model="query.operation" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="operation" label="操作描述" width="180" />
        <el-table-column prop="requestUrl" label="请求路径" />
        <el-table-column prop="method" label="请求方法" width="200" />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="responseCode" label="状态码" width="80">
          <template #default="{ row }">
            <el-tag :type="row.responseCode === 200 ? 'success' : 'danger'">{{ row.responseCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="100" />
        <el-table-column prop="createTime" label="操作时间" width="180" />
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getLogPage } from '@/api/log'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = ref({ pageNum: 1, pageSize: 10, username: '', operation: '' })

onMounted(() => { fetchData() })

async function fetchData() {
  loading.value = true
  try {
    const res = await getLogPage(query.value)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.value.pageNum = 1; fetchData() }
function resetQuery() { query.value = { pageNum: 1, pageSize: 10, username: '', operation: '' }; fetchData() }
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }
</style>
