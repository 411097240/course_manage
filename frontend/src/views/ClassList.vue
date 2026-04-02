<template>
  <div class="class-list">
    <div class="page-header">
      <h1 class="page-title">班级管理</h1>
      <el-button type="primary" @click="openDialog()" v-if="userStore.isAdmin()">+ 新建班级</el-button>
    </div>
    <div class="page-card">
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索班级名称或编码..." clearable @keyup.enter="loadData"
                  prefix-icon="Search" />
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" style="width:100%">
        <el-table-column prop="classCode" label="班级编码" min-width="160" />
        <el-table-column prop="className" label="班级名称" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="培训周期" min-width="200">
          <template #default="{ row }">
            <span v-if="row.startDate">{{ row.startDate }} ~ {{ row.endDate }}</span>
            <span v-else style="color:var(--text-muted)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="120">
          <template #default="{ row }">
            <span :class="['status-badge', row.status === 1 ? 'active' : 'inactive']">
              {{ row.status === 1 ? '进行中' : '已结束' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="300">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/class/${row.id}/course`)">排课</el-button>
            <el-button size="small" type="success" plain @click="$router.push(`/class/${row.id}/rollcall`)">点名记录</el-button>
            <el-button size="small" @click="openDialog(row)" v-if="userStore.isAdmin()">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)" v-if="userStore.isAdmin()">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:16px">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="current" :page-size="size" @current-change="loadData" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑班级' : '新建班级'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="form.className" placeholder="请输入班级名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="培训周期">
          <el-date-picker v-model="form.dateRange" type="daterange" range-separator="至"
                          start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
                          style="width:100%" />
        </el-form-item>
        <el-form-item label="状态" v-if="editId">
          <el-select v-model="form.status">
            <el-option label="进行中" :value="1" />
            <el-option label="已结束" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const tableData = ref([])
const loading = ref(false)
const keyword = ref('')
const current = ref(1)
const size = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const formRef = ref()
const form = reactive({ className: '', description: '', dateRange: null, status: 1 })
const rules = { className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }] }

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getClassList({ current: current.value, size: size.value, keyword: keyword.value })
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  editId.value = row ? row.id : null
  form.className = row ? row.className : ''
  form.description = row ? row.description : ''
  form.dateRange = (row && row.startDate) ? [row.startDate, row.endDate] : null
  form.status = row ? row.status : 1
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const submitData = {
      className: form.className,
      description: form.description,
      status: form.status,
      startDate: form.dateRange ? form.dateRange[0] : null,
      endDate: form.dateRange ? form.dateRange[1] : null
    }
    if (editId.value) {
      await api.updateClass({ id: editId.value, ...submitData })
      ElMessage.success('修改成功')
    } else {
      await api.addClass(submitData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除该班级吗？', '提示', { type: 'warning' }).then(async () => {
    await api.deleteClass(id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

onMounted(loadData)
</script>
