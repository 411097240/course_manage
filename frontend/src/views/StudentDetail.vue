<template>
  <div class="student-detail">
    <el-button text @click="$router.push('/student')" style="margin-bottom:12px;color:var(--text-muted)">
      ← 返回学生列表
    </el-button>

    <!-- 学生信息卡 -->
    <div class="info-card page-card" v-if="student">
      <div class="info-header">
        <div class="student-avatar">{{ student.name?.charAt(0) }}</div>
        <div>
          <h2>{{ student.name }}</h2>
          <p class="student-no">学号：{{ student.studentNo }}</p>
        </div>
        <span :class="['status-badge', student.status === 1 ? 'active' : 'inactive']" style="margin-left:auto">
          {{ student.status === 1 ? '启用' : '禁用' }}
        </span>
      </div>
      <div class="info-grid">
        <div class="info-item"><label>性别</label><span>{{ student.gender === 1 ? '男' : student.gender === 2 ? '女' : '-' }}</span></div>
        <div class="info-item"><label>手机号</label><span>{{ student.phone || '-' }}</span></div>
        <div class="info-item"><label>身份证号</label><span>{{ student.idCard || '-' }}</span></div>
      </div>
    </div>

    <!-- 已加入班级 -->
    <div class="page-card" style="margin-top:20px">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
        <h3 style="color:var(--text-primary)">📖 已加入班级</h3>
      </div>
      <el-table :data="classes" stripe v-loading="classLoading">
        <el-table-column prop="classCode" label="班级编码" width="150" />
        <el-table-column prop="className" label="班级名称" min-width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span :class="['status-badge', row.status === 1 ? 'active' : 'inactive']">
              {{ row.status === 1 ? '在读' : '已退出' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="入班时间" width="180">
          <template #default="{ row }">{{ formatTime(row.joinTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" size="small" type="danger" @click="handleLeave(row.classId)">出班</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 课程表 -->
    <div class="page-card" style="margin-top:20px">
      <h3 style="margin-bottom:16px;color:var(--text-primary)">📅 全部课程表</h3>
      <div v-if="schedule.length === 0 && !scheduleLoading" style="text-align:center;padding:40px;color:var(--text-muted)">
        暂无课程数据
      </div>
      <div v-else class="week-grid">
        <div class="week-col" v-for="day in 7" :key="day">
          <div class="week-header">{{ dayNames[day - 1] }}</div>
          <div class="week-body">
            <div v-for="course in getScheduleByDay(day)" :key="course.courseId" class="week-course-card">
              <div class="wc-time">{{ course.startTime }} - {{ course.endTime }}</div>
              <div class="wc-teacher" v-if="course.teacherName">👨‍🏫 {{ course.teacherName }}</div>
              <div class="wc-location" v-if="course.location">📍 {{ course.location }}</div>
              <div class="wc-class">{{ course.className }}</div>
            </div>
            <div v-if="getScheduleByDay(day).length === 0" class="week-empty">暂无</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const route = useRoute()
const studentId = Number(route.params.id)
const student = ref(null)
const classes = ref([])
const schedule = ref([])
const classLoading = ref(false)
const scheduleLoading = ref(false)
const dayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const formatTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

const getScheduleByDay = (day) => schedule.value.filter(c => c.dayOfWeek === day)

const loadData = async () => {
  const res = await api.getStudentById(studentId)
  student.value = res.data

  classLoading.value = true
  try {
    const cRes = await api.getStudentClasses(studentId)
    classes.value = cRes.data || []
  } finally {
    classLoading.value = false
  }

  scheduleLoading.value = true
  try {
    const sRes = await api.getStudentSchedule(studentId)
    schedule.value = sRes.data || []
  } finally {
    scheduleLoading.value = false
  }
}

const handleLeave = (classId) => {
  ElMessageBox.confirm('确定将该学生退出此班级吗？', '提示', { type: 'warning' }).then(async () => {
    await api.leaveClass({ studentId, classId })
    ElMessage.success('出班成功')
    loadData()
  }).catch(() => {})
}

onMounted(loadData)
</script>

<style scoped>
.info-card .info-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.student-avatar {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, #6366f1, #a78bfa);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: white;
}

.info-header h2 {
  font-size: 22px;
  color: var(--text-primary);
}

.student-no {
  color: var(--text-muted);
  font-size: 13px;
  margin-top: 2px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item label {
  font-size: 12px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-item span {
  font-size: 15px;
  color: var(--text-primary);
}

.week-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
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
  min-height: 100px;
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

.wc-class {
  font-size: 10px;
  color: var(--text-muted);
  margin-top: 4px;
  padding-top: 4px;
  border-top: 1px dashed var(--border-color);
}

.week-empty {
  text-align: center;
  color: var(--text-muted);
  font-size: 12px;
  padding: 20px 0;
}
</style>
