<template>
  <div class="payment-list">
    <div class="page-header">
      <h1 class="page-title">缴费管理</h1>
    </div>
    <div class="page-card">
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索姓名或学号..." clearable @keyup.enter="loadData"
                  prefix-icon="Search" style="max-width:240px" />
        <el-select v-model="paymentStatus" placeholder="缴费状态" clearable style="width:140px" @change="loadData">
          <el-option label="结余" :value="1" />
          <el-option label="结清" :value="2" />
          <el-option label="欠费" :value="3" />
        </el-select>
        <el-select v-model="classId" placeholder="班级筛选" clearable filterable style="width:200px" @change="loadData">
          <el-option v-for="c in classList" :key="c.id" :label="`${c.classCode} - ${c.className}`" :value="c.id" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" style="width:100%">
        <el-table-column prop="studentNo" label="学号" min-width="130" />
        <el-table-column prop="studentName" label="姓名" min-width="100" />
        <el-table-column prop="classCode" label="班级编码" min-width="130" />
        <el-table-column prop="className" label="班级名称" min-width="150" />
        <el-table-column label="应缴金额" min-width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
        </el-table-column>
        <el-table-column label="实收金额" min-width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.amountReceived) }}</template>
        </el-table-column>
        <el-table-column label="差额" min-width="110" align="right">
          <template #default="{ row }">
            <span :style="{ color: balanceColor(row.balance) }">{{ formatBalance(row.balance) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="缴费状态" min-width="100">
          <template #default="{ row }">
            <span :class="['status-badge', paymentStatusClass(row.paymentStatus)]">
              {{ paymentStatusLabel(row.paymentStatus) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" @click="$router.push(`/student/${row.studentId}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:16px">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="current" :page-size="size" @current-change="loadData" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="编辑缴费信息" width="480px" destroy-on-close>
      <div v-if="editRow" style="margin-bottom:16px;color:var(--text-secondary);font-size:14px">
        {{ editRow.studentName }}（{{ editRow.studentNo }}）— {{ editRow.className }}
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="应缴金额" prop="amountDue">
          <el-input-number v-model="form.amountDue" :min="0" :precision="2" :step="100" style="width:100%" />
        </el-form-item>
        <el-form-item label="实收金额" prop="amountReceived">
          <el-input-number v-model="form.amountReceived" :min="0" :precision="2" :step="100" style="width:100%" />
        </el-form-item>
        <el-form-item label="差额">
          <span :style="{ color: balanceColor(previewBalance), fontWeight: 600 }">{{ formatBalance(previewBalance) }}</span>
        </el-form-item>
        <el-form-item label="缴费状态">
          <span :class="['status-badge', paymentStatusClass(previewStatus)]">
            {{ paymentStatusLabel(previewStatus) }}
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const keyword = ref('')
const paymentStatus = ref(null)
const classId = ref(null)
const classList = ref([])

const dialogVisible = ref(false)
const editRow = ref(null)
const submitLoading = ref(false)
const formRef = ref(null)
const form = reactive({
  amountDue: 0,
  amountReceived: 0
})
const rules = {
  amountDue: [{ required: true, message: '请输入应缴金额', trigger: 'blur' }],
  amountReceived: [{ required: true, message: '请输入实收金额', trigger: 'blur' }]
}

const previewBalance = computed(() => {
  const due = form.amountDue ?? 0
  const received = form.amountReceived ?? 0
  return received - due
})

const previewStatus = computed(() => calcStatus(previewBalance.value))

const calcStatus = (balance) => {
  if (balance > 0) return 1
  if (balance < 0) return 3
  return 2
}

const formatMoney = (val) => {
  const n = Number(val ?? 0)
  return n.toFixed(2)
}

const formatBalance = (val) => {
  const n = Number(val ?? 0)
  const fixed = n.toFixed(2)
  if (n > 0) return `+${fixed}`
  return fixed
}

const balanceColor = (balance) => {
  const n = Number(balance ?? 0)
  if (n > 0) return '#22c55e'
  if (n < 0) return '#ef4444'
  return 'var(--text-primary)'
}

const paymentStatusLabel = (status) => {
  if (status === 1) return '结余'
  if (status === 3) return '欠费'
  return '结清'
}

const paymentStatusClass = (status) => {
  if (status === 1) return 'active'
  if (status === 3) return 'inactive'
  return ''
}

const loadClasses = async () => {
  const res = await api.getClassList({ current: 1, size: 999 })
  classList.value = res.data.records || []
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (paymentStatus.value) params.paymentStatus = paymentStatus.value
    if (classId.value) params.classId = classId.value
    const res = await api.getPaymentList(params)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const openEditDialog = (row) => {
  editRow.value = row
  form.amountDue = Number(row.amountDue ?? 0)
  form.amountReceived = Number(row.amountReceived ?? 0)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    await api.savePayment({
      studentClassId: editRow.value.studentClassId,
      amountDue: form.amountDue,
      amountReceived: form.amountReceived
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadClasses()
  loadData()
})
</script>
