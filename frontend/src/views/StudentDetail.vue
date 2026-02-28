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
        <span :class="['status-badge', student.status === 1 ? 'active' : 'inactive']" style="margin-left:auto;margin-right:16px;">
          {{ student.status === 1 ? '启用' : '禁用' }}
        </span>
        <el-button type="primary" @click="printVoucher">打印入班凭证</el-button>
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
        <el-table-column label="培训周期" min-width="200">
          <template #default="{ row }">
            <span v-if="row.startDate">{{ row.startDate }} ~ {{ row.endDate }}</span>
            <span v-else style="color:var(--text-muted)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="班级状态" width="100">
          <template #default="{ row }">
            <span :class="['status-badge', row.classStatus === 1 ? 'active' : 'inactive']">
              {{ row.classStatus === 1 ? '进行中' : '已结束' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="在读状态" width="100">
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
            <el-button v-if="row.status === 1 && row.classStatus === 1" size="small" type="danger" @click="handleLeave(row.classId)">出班</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 课程表 -->
    <div class="page-card" style="margin-top:20px">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
        <h3 style="color:var(--text-primary)">📅 课程表检索</h3>
        <el-date-picker v-model="selectedDate" type="date" placeholder="选择日期查看课表" value-format="YYYY-MM-DD" :clearable="false" />
      </div>
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
    
    <!-- 打印用的入班凭证模板 (仅在打印时可见) -->
    <div class="print-voucher-container">
      <div class="voucher-header">
        <h2>🎉 入班凭证 🎉</h2>
        <p>此凭证为学生上课依据，请妥善保管。</p>
      </div>

      <div class="voucher-section" v-if="student">
        <h3 class="section-title">学生基本信息</h3>
        <div class="info-row">
          <span>姓名：{{ student.name }}</span>
          <span>学号：{{ student.studentNo }}</span>
          <span>性别：{{ student.gender === 1 ? '男' : student.gender === 2 ? '女' : '-' }}</span>
          <span>手机：{{ student.phone || '-' }}</span>
        </div>
      </div>

      <div class="voucher-section" v-if="classes.length">
        <h3 class="section-title">入选班级</h3>
        <table class="voucher-table">
          <thead>
            <tr>
              <th>班级编码</th>
              <th>班级名称</th>
              <th>培训周期</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in classes.filter(cls => cls.status === 1)" :key="c.classId">
              <td>{{ c.classCode }}</td>
              <td>{{ c.className }}</td>
              <td>{{ c.startDate ? `${c.startDate} ~ ${c.endDate}` : '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="voucher-section" v-if="schedule.length">
        <h3 class="section-title">固定课程排期</h3>
        <table class="voucher-table">
          <thead>
            <tr>
              <th>星期</th>
              <th>时间</th>
              <th>授课教师</th>
              <th>上课地点</th>
              <th>所属班级</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="course in schedule" :key="course.courseId">
              <td>{{ dayNames[course.dayOfWeek - 1] }}</td>
              <td>{{ course.startTime }} - {{ course.endTime }}</td>
              <td>{{ course.teacherName || '-' }}</td>
              <td>{{ course.location || '-' }}</td>
              <td>{{ course.className }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <div class="voucher-footer">
        <p>生成时间: {{ new Date().toLocaleString() }}</p>
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

const selectedDate = ref(new Date().toISOString().split('T')[0])

const formatTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

const getScheduleByDay = (day) => {
  return schedule.value.filter(c => {
    // 1. 匹配星期
    if (c.dayOfWeek !== day) return false
    
    // 2. 匹配选择日期的有效性 (如果有班级起止时间，必须在周期内)
    // 根据所选日期推算出“day”（1-7）对应那天的实际日期字符串 (YYYY-MM-DD)
    const dateObj = new Date(selectedDate.value)
    let selectedDayOfWeek = dateObj.getDay() || 7 // 1-7 (周一直到周日)
    
    // 计算所在周的星期一的日期
    const diffToMonday = selectedDayOfWeek - 1
    const monday = new Date(dateObj.getTime() - diffToMonday * 24 * 60 * 60 * 1000)
    
    // 计算当前遍历到的那个格子 (day) 的实际日期
    const currentGridDateObj = new Date(monday.getTime() + (day - 1) * 24 * 60 * 60 * 1000)
    const currentGridDateStr = currentGridDateObj.toISOString().split('T')[0]
    
    // 3. 判断这个格子本身的日期有没有超过班级定义的周期
    if (c.startDate && c.endDate) {
      if (currentGridDateStr < c.startDate || currentGridDateStr > c.endDate) {
        return false
      }
    }

    return true
  })
}

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

const printVoucher = () => {
  window.print()
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

/* --- 打印样式区 --- */
.print-voucher-container {
  display: none; /* 平时隐藏 */
}

@media print {
  /* 隐藏系统所有无关UI，包括侧边栏、顶部导航以及本页卡片内容 */
  :root {
    --text-primary: #000;
  }
  
  body * {
    visibility: hidden;
  }

  .print-voucher-container, 
  .print-voucher-container * {
    visibility: visible;
  }

  .print-voucher-container {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    background: #fff;
    color: #000;
    padding: 20px;
    font-family: 'Helvetica Neue', Arial, sans-serif;
  }

  .voucher-header {
    text-align: center;
    border-bottom: 2px solid #ccc;
    padding-bottom: 20px;
    margin-bottom: 30px;
  }

  .voucher-header h2 {
    font-size: 28px;
    margin: 0 0 10px;
    display: inline-block;
  }

  .voucher-header p {
    color: #666;
    margin: 0;
  }

  .voucher-section {
    margin-bottom: 30px;
  }

  .section-title {
    font-size: 18px;
    margin-bottom: 15px;
    border-left: 4px solid #6366f1;
    padding-left: 10px;
  }

  .info-row {
    display: flex;
    flex-wrap: wrap;
    gap: 30px;
    font-size: 15px;
    background: #f8f9fa;
    padding: 15px;
    border-radius: 8px;
    border: 1px solid #ddd;
  }

  .voucher-table {
    width: 100%;
    border-collapse: collapse;
    margin-top: 10px;
  }

  .voucher-table th, 
  .voucher-table td {
    border: 1px solid #ccc;
    padding: 10px;
    text-align: center;
    font-size: 14px;
  }

  .voucher-table th {
    background-color: #f1f1f1;
    font-weight: bold;
  }

  .voucher-footer {
    margin-top: 50px;
    text-align: right;
    font-size: 13px;
    color: #888;
  }
}
</style>
