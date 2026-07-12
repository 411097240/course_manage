<template>
  <div class="classroom-list">
    <div class="page-header">
      <h1 class="page-title">教室管理</h1>
      <el-button type="primary" @click="openDialog()" v-if="userStore.isAdmin()">+ 新增教室</el-button>
    </div>
    <div class="page-card">
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索教室名称..." clearable @keyup.enter="loadData"
                  prefix-icon="Search" />
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" style="width:100%">
        <el-table-column prop="name" label="教室名称" min-width="200" />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <span :class="['status-badge', row.status === 1 ? 'active' : 'inactive']">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="280">
          <template #default="{ row }">
            <el-button size="small" @click="openCalendar(row)">查看预约</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑教室' : '新增教室'" width="460px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="教室名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入教室名称" />
        </el-form-item>
        <el-form-item label="状态" v-if="editId">
          <el-select v-model="form.status">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="calendarVisible" :title="`${viewClassroomName} - 预约情况`" width="920px" destroy-on-close>
      <div class="calendar-toolbar">
        <div style="display:flex;align-items:center;gap:12px">
          <span style="color:var(--text-secondary)">选择月份：</span>
          <el-date-picker
            v-model="calendarMonth"
            type="month"
            placeholder="选择月份"
            value-format="YYYY-MM"
            @change="onMonthChange"
          />
        </div>
        <el-button type="primary" @click="openReservationDialog()">+ 手动预约</el-button>
      </div>
      <el-calendar v-model="calendarDate">
        <template #date-cell="{ data }">
          <div class="calendar-cell">
            <span class="calendar-day">{{ data.day.split('-')[2] }}</span>
            <div class="calendar-reservations">
              <div
                v-for="item in getReservationsForDate(data.day)"
                :key="item.id"
                :class="['reservation-item', item.manual ? 'manual' : 'course']"
                :title="getReservationTitle(item)"
                @click.stop="handleReservationClick(item)"
              >
                {{ item.startTime }}-{{ item.endTime }}
                <span>{{ item.manual ? item.title : item.className }}</span>
              </div>
            </div>
          </div>
        </template>
      </el-calendar>
    </el-dialog>

    <el-dialog v-model="reservationDialogVisible" :title="reservationEditId ? '编辑手动预约' : '手动预约'" width="480px" destroy-on-close>
      <el-form ref="reservationFormRef" :model="reservationForm" :rules="reservationRules" label-width="90px">
        <el-form-item label="预约日期" prop="reserveDate">
          <el-date-picker
            v-model="reservationForm.reserveDate"
            type="date"
            placeholder="请选择日期"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-time-picker v-model="reservationForm.startTime" format="HH:mm" value-format="HH:mm" placeholder="开始" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-time-picker v-model="reservationForm.endTime" format="HH:mm" value-format="HH:mm" placeholder="结束" style="width:100%" />
        </el-form-item>
        <el-form-item label="预约事由" prop="title">
          <el-input v-model="reservationForm.title" placeholder="如：部门会议、活动彩排" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="reservationEditId" type="danger" @click="handleDeleteReservation">删除</el-button>
        <el-button @click="reservationDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reservationSubmitLoading" @click="handleReservationSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
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
const form = reactive({ name: '', status: 1 })
const rules = {
  name: [{ required: true, message: '请输入教室名称', trigger: 'blur' }]
}

const calendarVisible = ref(false)
const viewClassroomId = ref(null)
const viewClassroomName = ref('')
const calendarMonth = ref('')
const calendarDate = ref(new Date())
const reservations = ref([])

const reservationDialogVisible = ref(false)
const reservationEditId = ref(null)
const reservationSubmitLoading = ref(false)
const reservationFormRef = ref()
const reservationForm = reactive({
  reserveDate: '',
  startTime: '',
  endTime: '',
  title: ''
})
const reservationRules = {
  reserveDate: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  title: [{ required: true, message: '请输入预约事由', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getClassroomList({ current: current.value, size: size.value, keyword: keyword.value })
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  editId.value = row ? row.id : null
  form.name = row ? row.name : ''
  form.status = row ? row.status : 1
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (editId.value) {
      await api.updateClassroom({ id: editId.value, ...form })
      ElMessage.success('修改成功')
    } else {
      await api.addClassroom(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除该教室吗？', '提示', { type: 'warning' }).then(async () => {
    await api.deleteClassroom(id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

const getMonthRange = (monthStr) => {
  const [year, month] = monthStr.split('-').map(Number)
  const startDate = `${monthStr}-01`
  const lastDay = new Date(year, month, 0).getDate()
  const endDate = `${monthStr}-${String(lastDay).padStart(2, '0')}`
  return { startDate, endDate }
}

const loadReservations = async () => {
  if (!viewClassroomId.value || !calendarMonth.value) return
  const { startDate, endDate } = getMonthRange(calendarMonth.value)
  const res = await api.getClassroomReservations(viewClassroomId.value, { startDate, endDate })
  reservations.value = res.data || []
}

const onMonthChange = () => {
  if (calendarMonth.value) {
    calendarDate.value = new Date(`${calendarMonth.value}-01`)
  }
  loadReservations()
}

watch(calendarDate, (val) => {
  if (!calendarVisible.value || !val) return
  const monthStr = `${val.getFullYear()}-${String(val.getMonth() + 1).padStart(2, '0')}`
  if (monthStr !== calendarMonth.value) {
    calendarMonth.value = monthStr
    loadReservations()
  }
})

const openCalendar = async (row) => {
  viewClassroomId.value = row.id
  viewClassroomName.value = row.name
  const now = new Date()
  calendarMonth.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  calendarDate.value = new Date(`${calendarMonth.value}-01`)
  calendarVisible.value = true
  await loadReservations()
}

const getReservationsForDate = (dateStr) => {
  return reservations.value.filter(r => r.reserveDate === dateStr)
}

const getReservationTitle = (item) => {
  if (item.manual) {
    return `${item.startTime}-${item.endTime} ${item.title || ''}`
  }
  return `${item.startTime}-${item.endTime} ${item.className || ''}`
}

const openReservationDialog = (row, dateStr) => {
  reservationEditId.value = row ? row.id : null
  reservationForm.reserveDate = row ? row.reserveDate : (dateStr || '')
  reservationForm.startTime = row ? row.startTime : ''
  reservationForm.endTime = row ? row.endTime : ''
  reservationForm.title = row ? row.title : ''
  reservationDialogVisible.value = true
}

const handleReservationClick = (item) => {
  if (item.manual) {
    openReservationDialog(item)
  } else {
    ElMessage.info('该预约由排课自动生成，请在课程管理中修改')
  }
}

const handleReservationSubmit = async () => {
  const valid = await reservationFormRef.value.validate().catch(() => false)
  if (!valid) return
  reservationSubmitLoading.value = true
  try {
    const payload = {
      classroomId: viewClassroomId.value,
      reserveDate: reservationForm.reserveDate,
      startTime: reservationForm.startTime,
      endTime: reservationForm.endTime,
      title: reservationForm.title
    }
    if (reservationEditId.value) {
      await api.updateManualReservation({ id: reservationEditId.value, ...payload })
      ElMessage.success('修改成功')
    } else {
      await api.addManualReservation(payload)
      ElMessage.success('预约成功')
    }
    reservationDialogVisible.value = false
    loadReservations()
  } finally {
    reservationSubmitLoading.value = false
  }
}

const handleDeleteReservation = () => {
  ElMessageBox.confirm('确定删除该手动预约吗？', '提示', { type: 'warning' }).then(async () => {
    await api.deleteManualReservation(reservationEditId.value)
    ElMessage.success('删除成功')
    reservationDialogVisible.value = false
    loadReservations()
  }).catch(() => {})
}

onMounted(loadData)
</script>

<style scoped>
.calendar-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 12px;
}

:deep(.el-calendar) {
  --el-calendar-border: var(--border-color);
  --el-calendar-selected-bg-color: rgba(99, 102, 241, 0.2);
  background: var(--bg-dark);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
}

:deep(.el-calendar__header) {
  border-bottom: 1px solid var(--border-color);
  padding: 12px 16px;
}

:deep(.el-calendar__button-group .el-button) {
  --el-button-bg-color: var(--bg-card);
  --el-button-border-color: var(--border-color);
  --el-button-text-color: var(--text-primary);
  --el-button-hover-bg-color: rgba(99, 102, 241, 0.1);
  --el-button-hover-border-color: var(--primary);
  --el-button-hover-text-color: var(--primary-light);
}

:deep(.el-calendar-table thead th) {
  color: var(--text-secondary);
  font-weight: 500;
  border-bottom: 1px solid var(--border-color);
}

:deep(.el-calendar-table td) {
  border-color: var(--border-color);
}

:deep(.el-calendar-table .el-calendar-day) {
  height: 90px;
  padding: 0;
  background: var(--bg-card);
}

:deep(.el-calendar-table td.is-today .el-calendar-day) {
  background: rgba(99, 102, 241, 0.12);
}

:deep(.el-calendar-table td.is-selected .el-calendar-day) {
  background: rgba(99, 102, 241, 0.2);
}

.calendar-cell {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 4px;
  overflow: hidden;
}

.calendar-day {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

:deep(.el-calendar-table td.prev .calendar-day),
:deep(.el-calendar-table td.next .calendar-day) {
  color: var(--text-muted);
}

.calendar-reservations {
  flex: 1;
  overflow-y: auto;
  margin-top: 4px;
}

.reservation-item {
  font-size: 11px;
  line-height: 1.4;
  padding: 2px 4px;
  margin-bottom: 2px;
  border-radius: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}

.reservation-item.course {
  background: rgba(99, 102, 241, 0.25);
  color: var(--text-primary);
}

.reservation-item.manual {
  background: rgba(16, 185, 129, 0.25);
  color: var(--text-primary);
}

:deep(.el-calendar-table td.is-selected .calendar-day) {
  color: var(--primary-light);
}
</style>
