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

    <!-- 周视图 -->
    <div class="page-card" style="margin-bottom:24px">
      <h3 style="margin-bottom:16px;color:var(--text-primary)">📅 周课程表</h3>
      <div class="week-grid">
        <div class="week-col" v-for="day in 7" :key="day">
          <div class="week-header">{{ dayNames[day - 1] }}</div>
          <div class="week-body">
            <div v-for="course in getCoursesByDay(day)" :key="course.id" class="week-course-card"
                 @click="openDialog(course)">
              <div class="wc-time">{{ course.startTime }} - {{ course.endTime }}</div>
              <div class="wc-teacher" v-if="course.teacherName">👨‍🏫 {{ course.teacherName }}</div>
              <div class="wc-location" v-if="course.location">📍 {{ course.location }}</div>
            </div>
            <div v-if="getCoursesByDay(day).length === 0" class="week-empty">暂无</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 列表视图 -->
    <div class="page-card">
      <h3 style="margin-bottom:16px;color:var(--text-primary)">📋 课程列表</h3>
      <el-table :data="courses" stripe v-loading="loading">
        <el-table-column label="星期" width="100">
          <template #default="{ row }">{{ dayNames[row.dayOfWeek - 1] }}</template>
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
        <el-form-item label="星期" prop="dayOfWeek">
          <el-select v-model="form.dayOfWeek" placeholder="请选择星期">
            <el-option v-for="(name, i) in dayNames" :key="i" :label="name" :value="i + 1" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-time-picker v-model="form.startTime" format="HH:mm" value-format="HH:mm" placeholder="开始" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-time-picker v-model="form.endTime" format="HH:mm" value-format="HH:mm" placeholder="结束" />
        </el-form-item>
        <el-form-item label="上课地点">
          <el-input v-model="form.location" placeholder="请输入上课地点" />
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
const loading = ref(false)
const dayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const dialogVisible = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const formRef = ref()
const form = reactive({ teacherName: '', dayOfWeek: null, startTime: '', endTime: '', location: '' })
const rules = {
  dayOfWeek: [{ required: true, message: '请选择星期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const [classRes, courseRes] = await Promise.all([
      api.getClassById(classId),
      api.getCourseList(classId)
    ])
    classInfo.value = classRes.data || {}
    courses.value = courseRes.data || []
  } finally {
    loading.value = false
  }
}

const getCoursesByDay = (day) => courses.value.filter(c => c.dayOfWeek === day)

const openDialog = (row) => {
  editId.value = row ? row.id : null
  form.teacherName = row ? row.teacherName : ''
  form.dayOfWeek = row ? row.dayOfWeek : null
  form.startTime = row ? row.startTime : ''
  form.endTime = row ? row.endTime : ''
  form.location = row ? row.location : ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (editId.value) {
      await api.updateCourse({ id: editId.value, classId, ...form })
      ElMessage.success('修改成功')
    } else {
      await api.addCourse({ classId, ...form })
      ElMessage.success('添加成功')
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

<style scoped>
.week-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.week-col {
  min-width: 0;
}

.week-header {
  text-align: center;
  padding: 10px 0;
  font-weight: 600;
  font-size: 13px;
  color: var(--primary-light);
  background: rgba(99, 102, 241, 0.08);
  border-radius: 8px 8px 0 0;
}

.week-body {
  min-height: 120px;
  padding: 8px 4px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.week-course-card {
  padding: 8px 10px;
  background: rgba(99, 102, 241, 0.1);
  border: 1px solid rgba(99, 102, 241, 0.15);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.week-course-card:hover {
  background: rgba(99, 102, 241, 0.18);
  transform: translateY(-1px);
}

.wc-name {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.wc-time {
  font-size: 11px;
  color: var(--primary-light);
}

.wc-teacher, .wc-location {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

.week-empty {
  text-align: center;
  color: var(--text-muted);
  font-size: 12px;
  padding: 20px 0;
}
</style>
