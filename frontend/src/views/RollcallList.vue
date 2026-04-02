<template>
  <div class="rollcall-list">
    <div class="page-header">
      <div class="header-content">
        <h2>📋 点名管理 - {{ className }}</h2>
        <p>查看并管理本班级的出勤记录数据</p>
      </div>
      <div>
        <el-button @click="$router.push('/class')">返回班级</el-button>
      </div>
    </div>

    <div class="page-card">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="recordDate" label="点名日期" min-width="120" />
        <el-table-column prop="teacherName" label="点名教师" min-width="120" />
        <el-table-column label="创建时间" min-width="150">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:16px">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="current" :page-size="size" @current-change="loadData" />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="点名详情" width="600px">
      <el-table :data="details" v-loading="detailLoading" height="400">
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="studentName" label="姓名" width="150" />
        <el-table-column label="出勤状态">
          <template #default="{ row }">
            <el-tag v-if="row.status===1" type="success">出勤</el-tag>
            <el-tag v-else-if="row.status===2" type="warning">迟到</el-tag>
            <el-tag v-else-if="row.status===3" type="info">请假</el-tag>
            <el-tag v-else type="danger">缺勤</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'

const route = useRoute()
const router = useRouter()
const classId = route.params.id

const tableData = ref([])
const loading = ref(false)
const current = ref(1)
const size = ref(10)
const total = ref(0)
const className = ref('')

const detailVisible = ref(false)
const detailLoading = ref(false)
const details = ref([])

const formatTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

const loadData = async () => {
  loading.value = true
  try {
    const classRes = await api.getClassById(classId)
    className.value = classRes.data.className

    const res = await api.getRollcallRecords({ classId, current: current.value, size: size.value })
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

const openDetail = async (recordId) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await api.getRollcallDetail(recordId)
    details.value = res.data
  } finally {
    detailLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.header-content h2 { font-size: 24px; color: var(--text-primary); margin:0;}
.header-content p { color: var(--text-muted); font-size: 14px; margin-top: 4px; }
.page-card {
  border-radius: 16px;
  padding: 24px;
  background: white;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}
</style>
