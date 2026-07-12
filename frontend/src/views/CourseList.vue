<template>
  <div class="course-list">
    <div class="page-header">
      <div>
        <el-button text @click="$router.push('/class')" style="margin-bottom:8px;color:var(--text-muted)">
          ← 返回班级列表
        </el-button>
        <h1 class="page-title">{{ classInfo.className || '课程排课' }}
          <span v-if="classInfo.classCode" style="font-size:14px;font-weight:400;color:var(--text-muted);margin-left:12px">
            {{ classInfo.classCode }}
          </span>
        </h1>
      </div>
      <el-button type="primary" @click="openDialog()">+ 新增课程</el-button>
    </div>

    <!-- 列表视图 -->
    <div class="page-card">
      <h3 style="margin-bottom:16px;color:var(--text-primary)">📋 课程列表</h3>
      <el-table :data="courses" v-loading="loading">
        <el-table-column label="日期" width="160">
          <template #default="{ row }">{{ formatDateLabel(row.courseDate) }}</template>
        </el-table-column>
        <el-table-column prop="teacherName" label="授课教师" width="120" />
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ row.startTime }} - {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column prop="location" label="上课地点" width="140" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑课程' : '新增课程'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="授课教师">
          <el-input v-model="form.teacherName" placeholder="请输入授课教师" />
        </el-form-item>
        <el-form-item v-if="editId" label="上课日期" prop="courseDate">
          <el-date-picker
            v-model="form.courseDate"
            type="date"
            placeholder="请选择日期"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledDate"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item v-else label="上课日期" prop="courseDates">
          <el-date-picker
            v-model="form.courseDates"
            type="dates"
            placeholder="请选择一个或多个日期"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledDate"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-time-picker v-model="form.startTime" format="HH:mm" value-format="HH:mm" placeholder="开始" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-time-picker v-model="form.endTime" format="HH:mm" value-format="HH:mm" placeholder="结束" />
        </el-form-item>
        <el-form-item label="上课地点">
          <el-select v-model="form.classroomId" placeholder="请选择教室" filterable clearable style="width:100%">
            <el-option v-for="c in classrooms" :key="c.id" :label="c.name" :value="c.id" />
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
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const route = useRoute()
const classId = Number(route.params.id)
const classInfo = ref({})
const courses = ref([])
const classrooms = ref([])
const loading = ref(false)
const dayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

const dialogVisible = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const formRef = ref()
const form = reactive({
  teacherName: '',
  courseDate: '',
  courseDates: [],
  startTime: '',
  endTime: '',
  classroomId: null
})

const rules = {
  courseDate: [{ required: true, message: '请选择上课日期', trigger: 'change' }],
  courseDates: [{
    validator: (_, value, callback) => {
      if (!value || value.length === 0) callback(new Error('请至少选择一个上课日期'))
      else callback()
    },
    trigger: 'change'
  }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

const formatDateLabel = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const day = dayNames[d.getDay()]
  return `${dateStr} (${day})`
}

const disabledDate = (date) => {
  const { startDate, endDate } = classInfo.value
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const dateStr = `${y}-${m}-${d}`
  if (startDate && dateStr < startDate) return true
  if (endDate && dateStr > endDate) return true
  return false
}

const loadData = async () => {
  loading.value = true
  try {
    const [classRes, courseRes, classroomRes] = await Promise.all([
      api.getClassById(classId),
      api.getCourseList(classId),
      api.getAllClassrooms()
    ])
    classInfo.value = classRes.data || {}
    courses.value = courseRes.data || []
    classrooms.value = classroomRes.data || []
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  editId.value = row ? row.id : null
  form.teacherName = row ? row.teacherName : ''
  form.courseDate = row ? row.courseDate : ''
  form.courseDates = []
  form.startTime = row ? row.startTime : ''
  form.endTime = row ? row.endTime : ''
  form.classroomId = row ? row.classroomId : null
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (editId.value) {
      await api.updateCourse({
        id: editId.value,
        classId,
        teacherName: form.teacherName,
        courseDate: form.courseDate,
        startTime: form.startTime,
        endTime: form.endTime,
        classroomId: form.classroomId
      })
      ElMessage.success('修改成功')
    } else {
      await api.addCourseBatch({
        classId,
        courseDates: form.courseDates,
        teacherName: form.teacherName,
        startTime: form.startTime,
        endTime: form.endTime,
        classroomId: form.classroomId
      })
      ElMessage.success(`成功添加 ${form.courseDates.length} 条课程`)
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除该课程吗？', '提示', { type: 'warning' }).then(async () => {
    await api.deleteCourse(id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

onMounted(loadData)
</script>
